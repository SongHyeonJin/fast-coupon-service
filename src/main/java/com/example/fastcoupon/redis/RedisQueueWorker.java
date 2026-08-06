package com.example.fastcoupon.redis;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.dto.coupon.CouponQueueEventDto;
import com.example.fastcoupon.enums.CouponIssueEnum;
import com.example.fastcoupon.kafka.CouponIssueProducer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQueueWorker {

    private final CouponIssueProducer couponIssueProducer;
    private final RedisCouponService redisService;
    private final StringRedisTemplate redisTemplate;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @EventListener
    public void onCouponQueued(CouponQueueEventDto event) {
        long couponId = event.getCouponId();
        String cleanKey = String.format("coupon:%d:done", couponId);
        String lockKey = String.format("coupon:%d:running", couponId);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(cleanKey))) {
            log.info("✅ 이미 발급 완료된 쿠폰: couponId={}", couponId);
            return;
        }

        // 동일 쿠폰 워커 중복 실행 방지 (멀티 서버 대비용)
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "true", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.info("⛔ 워커 이미 실행 중: couponId={}", couponId);
            return;
        }

        executor.submit(() -> {
            try {
                processQueue(couponId, cleanKey);
            } finally {
                redisTemplate.delete(lockKey); // 워커 종료 시 락 해제
            }
        });
    }

    private void processQueue(long couponId, String cleanKey) {
        int total = redisService.getTotalCount(couponId);
        int current = redisService.getCurrentCount(couponId);
        String queueKey = String.format("coupon:%d:queue", couponId);

        String data = redisService.blockingPopQueue(couponId);
        if (data == null) {
            Long queueSize = redisTemplate.opsForList().size(queueKey);
            if (current >= total && (queueSize == null || queueSize == 0)) {
                log.info("🎯 couponId={} 발급 완료({}/{}) - 워커 종료", couponId, current, total);
                redisTemplate.opsForValue().set(cleanKey, "done", Duration.ofMinutes(10));
                cleanupCouponData(couponId);
            } else {
                log.info("⏳ 대기 시간 초과, 아직 남은 수량 있음: couponId={}", couponId);
            }
            return;
        }

        long userId = Long.parseLong(data.split(":")[1]);
        CouponIssueEnum result = redisService.tryIssueCoupon(couponId, userId, total);

        switch (result) {
            case SUCCESS -> {
                log.info("✅ 발급 성공: couponId={}, userId={}", couponId, userId);
                couponIssueProducer.send("coupon.issue", String.valueOf(couponId),
                        new CouponIssueEventDto(couponId, userId));
            }
            case OUT_OF_STOCK -> log.info("🎯 재고 소진: couponId={}", couponId);
            case ALREADY_ISSUED -> log.warn("🚫 중복 발급 시도: couponId={}, userId={}", couponId, userId);
            default -> log.error("❌ 예기치 않은 결과: {} for couponId={} userId={}", result, couponId, userId);
        }

        processQueue(couponId, cleanKey);
    }

    private void cleanupCouponData(long couponId) {
        String queueKey = String.format("coupon:%d:queue", couponId);
        log.info("✅ Redis 정리 시작: couponId={}", couponId);

        redisTemplate.delete(queueKey);
        String userPattern = String.format("coupon:%d:user:*", couponId);
        Set<String> userKeys = redisTemplate.keys(userPattern);
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }

        redisTemplate.delete(Arrays.asList(
                String.format("coupon:%d:total", couponId),
                String.format("coupon:%d:expire", couponId),
                String.format("coupon:%d:count", couponId)
        ));

        log.info("🧹 Redis 정리 완료: couponId={}", couponId);
    }

    @PreDestroy
    public void stopWorker() {
        executor.shutdown();
        log.info("🛑 RedisQueueWorker 종료됨");
    }
}
