package com.example.fastcoupon.redis;

import com.example.fastcoupon.dto.coupon.CouponQueueEventDto;
import com.example.fastcoupon.enums.CouponIssueEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponService {
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher publisher;

    private static final String COUPON_COUNT_KEY = "coupon:%d:count";
    private static final String USER_ISSUED_KEY = "coupon:%d:user:%d";
    private static final String COUPON_QUEUE_KEY = "coupon:%d:queue";
    private static final String COUPON_TOTAL_KEY = "coupon:%d:total";
    private static final String COUPON_EXPIRE_KEY   = "coupon:%d:expire";

    private String getQueueKey(Long couponId) {
        return String.format(COUPON_QUEUE_KEY, couponId);
    }

    public int getCurrentCount(long couponId) {
        String value = redisTemplate.opsForValue()
                .get(String.format(COUPON_COUNT_KEY, couponId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public int getTotalCount(long couponId) {
        String value = redisTemplate.opsForValue()
                .get(String.format(COUPON_TOTAL_KEY, couponId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    private static final String LUA_SCRIPT = """
        local userKey = KEYS[1]
        local countKey = KEYS[2]
        local total = tonumber(ARGV[1])
        local ttl = tonumber(ARGV[2])
        
        -- 이미 발급받은 유저인지 확인
        if redis.call("EXISTS", userKey) == 1 then
            return 1 -- 중복 발급
        end
        
        -- 현재 발급된 수 확인
        local current = tonumber(redis.call("GET", countKey) or "0")
        if current >= total then
            return 2 -- 재고 없음
        end
        
        -- 발급 처리 (카운트 증가 + userKey 설정 + TTL 적용)
        redis.call("INCR", countKey)
        redis.call("SET", userKey, "true", "EX", ttl)
        return 0 -- 발급 성공
    """;

    public CouponIssueEnum tryIssueCoupon(Long couponId, Long userId, int totalCount) {
        String userKey  = String.format(USER_ISSUED_KEY, couponId, userId);
        String countKey = String.format(COUPON_COUNT_KEY, couponId);
        String expireKey = String.format(COUPON_EXPIRE_KEY, couponId);

        Long ttlSeconds = redisTemplate.getExpire(expireKey, TimeUnit.SECONDS);
        long ttl = (ttlSeconds == null || ttlSeconds < 0) ? 0 : ttlSeconds;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                Arrays.asList(userKey, countKey),
                String.valueOf(totalCount),
                String.valueOf(ttl)
        );
        if (result == null) return CouponIssueEnum.FAIL;

        return switch (result.intValue()) {
            case 0 -> CouponIssueEnum.SUCCESS;
            case 1 -> CouponIssueEnum.ALREADY_ISSUED;
            case 2 -> CouponIssueEnum.OUT_OF_STOCK;
            default -> CouponIssueEnum.FAIL;
        };
    }

    // userKey가 실제로 있을 때만 되돌리므로 중복 호출/정리 후 호출에도 count가 음수가 되지 않는다.
    private static final String ROLLBACK_SCRIPT = """
        if redis.call("DEL", KEYS[1]) == 1 and redis.call("EXISTS", KEYS[2]) == 1 then
            redis.call("DECR", KEYS[2])
            return 1
        end
        return 0
    """;

    /** Kafka 발행 실패 시 tryIssueCoupon의 효과(count 증가 + userKey)를 원자적으로 되돌린다. */
    public boolean rollbackIssue(Long couponId, Long userId) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(ROLLBACK_SCRIPT, Long.class),
                Arrays.asList(String.format(USER_ISSUED_KEY, couponId, userId),
                        String.format(COUPON_COUNT_KEY, couponId)));
        return Long.valueOf(1L).equals(result);
    }

    public void pushQueue(Long couponId, Long userId) {
        String userKey = String.format(USER_ISSUED_KEY, couponId, userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            log.warn("🚫 이미 발급된 유저, 큐에 안넣음: couponId={}, userId={}", couponId, userId);
            return;
        }

        if (getCurrentCount(couponId) >= getTotalCount(couponId)) {
            log.warn("🎯 재고 소진 상태, 큐 적재 안 함: couponId={}, userId={}", couponId, userId);
            return;
        }

        String value = couponId + ":" + userId;
        redisTemplate.opsForList().rightPush(getQueueKey(couponId), value);
        log.info("✅ Redis 큐에 발급 요청 등록: {}", value);

        publisher.publishEvent(new CouponQueueEventDto(couponId, getTotalCount(couponId)));
    }

    public String blockingPopQueue(Long couponId) {
        try {
            return redisTemplate.opsForList()
                    .leftPop(getQueueKey(couponId), Duration.ofSeconds(5));
        } catch (Exception e) {
            return null;
        }
    }
}
