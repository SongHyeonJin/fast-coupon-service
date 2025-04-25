package com.example.fastcoupon.redis;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.enums.CouponIssueEnum;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCouponService {

    private final StringRedisTemplate redisTemplate;
    private final CouponRepository couponRepository;

    private static final String COUPON_COUNT_KEY_PREFIX = "coupon:%d:count";
    private static final String USER_ISSUED_KEY_PREFIX = "coupon:%d:user:%d";
    private static final String COUPON_QUEUE_KEY_FORMAT = "coupon:%d:queue";
    private static final String COUPON_TOTAL_KEY_PREFIX = "coupon:%d:total";

    private String getQueueKey(Long couponId) {
        return String.format(COUPON_QUEUE_KEY_FORMAT, couponId);
    }

    public int getCurrentCount(Long couponId) {
        String countKey = String.format(COUPON_COUNT_KEY_PREFIX, couponId);
        String countValue = redisTemplate.opsForValue().get(countKey);
        return countValue == null ? 0 : Integer.parseInt(countValue);
    }

    public int getTotalCount(Long couponId) {
        String totalKey = String.format(COUPON_TOTAL_KEY_PREFIX, couponId);
        String value = redisTemplate.opsForValue().get(totalKey);
        if (value != null) {
            return Integer.parseInt(value);
        }
        // Redis에 아직 total이 설정되지 않은 경우 DB에서 가져와 저장
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND));
        int total = coupon.getTotalQuantity();
        redisTemplate.opsForValue().set(totalKey, String.valueOf(total));
        return total;
    }

    // Lua 스크립트 정의 (원자적 실행)
    private static final String LUA_SCRIPT = """
          local userKey = KEYS[1]
          local countKey = KEYS[2]
          local total = tonumber(ARGV[1])
          
          -- 이미 발급받은 유저인지 확인
          if redis.call("EXISTS", userKey) == 1 then
              return 1 -- 중복 발급
          end
          
          -- 현재 발급된 수 확인 (get으로 count 조회)
          local current = tonumber(redis.call("GET", countKey) or "0")
          if current >= total then
              return 2 -- 재고 없음
          end
          
          -- 발급 처리
          redis.call("INCR", countKey)
          redis.call("SET", userKey, "true", "EX", 300)
          return 0 -- 발급 성공
    """;

    public CouponIssueEnum tryIssueCoupon(Long couponId, Long userId, int totalCount) {
        String userKey = String.format(USER_ISSUED_KEY_PREFIX, couponId, userId);
        String countKey = String.format(COUPON_COUNT_KEY_PREFIX, couponId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                Arrays.asList(userKey, countKey),
                String.valueOf(totalCount)
        );

        if (result == null) return CouponIssueEnum.FAIL;

        log.info("[Redis] Lua 실행 결과: {}, couponId={}, userId={}", result, couponId, userId);

        return switch (result.intValue()) {
            case 1 -> CouponIssueEnum.ALREADY_ISSUED;
            case 2 -> CouponIssueEnum.OUT_OF_STOCK;
            default -> CouponIssueEnum.SUCCESS;
        };
    }

    public void pushQueue(Long couponId, Long userId) {
        String userKey = String.format(USER_ISSUED_KEY_PREFIX, couponId, userId);

        // 중복 체크: 이미 발급된 유저는 큐에 넣지 않음
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            log.warn("🚫 Redis 큐 중복 삽입 방지: couponId={}, userId={}", couponId, userId);
            return;
        }

        String value = couponId + ":" + userId;
        redisTemplate.opsForList().rightPush(getQueueKey(couponId), value);
        log.info("✅ Redis 큐에 발급 요청 등록: {}", value);
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
