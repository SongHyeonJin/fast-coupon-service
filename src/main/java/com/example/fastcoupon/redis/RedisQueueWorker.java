package com.example.fastcoupon.redis;

import com.example.fastcoupon.enums.CouponIssueEnum;
import com.example.fastcoupon.service.CouponIssueService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQueueWorker {

    private final CouponIssueService couponIssueService;
    private final RedisCouponService redisService;
    private final StringRedisTemplate redisTemplate;

    private final Set<Long> startedCoupons = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private static final String ACTIVE_COUPON_SET_KEY = "coupon:active:ids";

    @PostConstruct
    public void startWorker() {
        scheduler.scheduleAtFixedRate(() -> {
            Set<String> active = redisTemplate.opsForSet().members(ACTIVE_COUPON_SET_KEY);
            if (active == null || active.isEmpty()) return;
            for (String id : active) {
                long couponId = Long.parseLong(id);
                if (!startedCoupons.add(couponId)) continue;
                Executors.newSingleThreadExecutor().submit(() -> processQueue(couponId));
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void processQueue(long couponId) {
        int total = redisService.getTotalCount(couponId);
        while (running.get()) {
            int current = redisService.getCurrentCount(couponId);
            if (current >= total) {
                log.info("🎯 couponId={} 이미 모두 처리됨({}/{}) - 워커 종료", couponId, current, total);
                startedCoupons.remove(couponId);

                redisTemplate.opsForSet().remove("coupon:active:ids", String.valueOf(couponId));
                redisTemplate.delete(Arrays.asList(
                        "coupon:" + couponId + ":count",
                        "coupon:" + couponId + ":total",
                        "coupon:" + couponId + ":queue"
                ));
                return;
            }

            String data = redisService.blockingPopQueue(couponId);
            if (data == null) continue;
            long userId = Long.parseLong(data.split(":")[1]);

            CouponIssueEnum result = redisService.tryIssueCoupon(couponId, userId, total);
            switch (result) {
                case SUCCESS -> {
                    couponIssueService.sendIssueEvent(couponId, userId);
                }
                case OUT_OF_STOCK -> {
                    log.info("🎯 재고 소진, worker 종료: couponId={} total={}", couponId, total);
                    return;
                }
                case ALREADY_ISSUED -> log.warn("🚫 중복 발급 시도 무시: couponId={} userId={}", couponId, userId);
                default -> log.error("❌ 예기치 않은 결과: {} for couponId={} userId={}", result, couponId, userId);
            }
        }
    }

    @PreDestroy
    public void stopWorker() {
        running.set(false);
        scheduler.shutdown();
        startedCoupons.clear();
        log.info("🛑 RedisQueueWorker 종료됨");
    }
}