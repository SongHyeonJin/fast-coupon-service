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
import org.springframework.scheduling.annotation.Scheduled;
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

    private static final String ACTIVE_COUPON_SET_KEY = "coupon:active:ids";

    @EventListener
    public void onCouponQueued(CouponQueueEventDto event) {
        tryStartWorker(event.getCouponId());
    }

    // tryStartWorker의 자가 치유는 "실제로 뜬 워커가 자기 작업을 끝내는 순간"에만
    // 재확인한다. 워커가 도중에 죽어서(서버 재시작, OOM 등) 락만 남고 아무도
    // 반납하지 않는 경우엔 그 재확인 자체가 일어나지 않는다. 락 없이 큐만 남아있는
    // 활성 쿠폰을 주기적으로 훑어 재시작하는 안전망을 둔다.
    @Scheduled(fixedDelay = 3000)
    public void sweepStuckQueues() {
        Set<String> activeIds = redisTemplate.opsForSet().members(ACTIVE_COUPON_SET_KEY);
        if (activeIds == null || activeIds.isEmpty()) return;

        for (String idStr : activeIds) {
            long couponId = Long.parseLong(idStr);
            String cleanKey = String.format("coupon:%d:done", couponId);
            String lockKey = String.format("coupon:%d:running", couponId);
            String queueKey = String.format("coupon:%d:queue", couponId);

            if (Boolean.TRUE.equals(redisTemplate.hasKey(cleanKey))) continue;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) continue; // 이미 워커가 돌고 있음

            Long queueSize = redisTemplate.opsForList().size(queueKey);
            if (queueSize != null && queueSize > 0) {
                log.warn("🩹 방치된 큐 발견, 워커 재시작: couponId={}, 대기={}", couponId, queueSize);
                tryStartWorker(couponId);
            }
        }
    }

    private void tryStartWorker(long couponId) {
        String cleanKey = String.format("coupon:%d:done", couponId);
        String lockKey = String.format("coupon:%d:running", couponId);
        String queueKey = String.format("coupon:%d:queue", couponId);

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

                // 락을 막 반납한 틈에 새로 들어온 요청은 이벤트가 드롭된 채 큐에만
                // 남아있을 수 있다. 여기서 한 번 더 확인해 스스로 재시작함으로써
                // 아무도 다시 깨워주지 않아 요청이 영구 방치되는 것을 막는다.
                Long remaining = redisTemplate.opsForList().size(queueKey);
                if (remaining != null && remaining > 0 && !Boolean.TRUE.equals(redisTemplate.hasKey(cleanKey))) {
                    tryStartWorker(couponId);
                }
            }
        });
    }

    private void processQueue(long couponId, String cleanKey) {
        String queueKey = String.format("coupon:%d:queue", couponId);
        boolean isDone = false;

        while (!isDone) {
            int total = redisService.getTotalCount(couponId);
            int current = redisService.getCurrentCount(couponId);

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
                isDone = true;
            } else {
                long userId = Long.parseLong(data.split(":")[1]);
                CouponIssueEnum result = redisService.tryIssueCoupon(couponId, userId, total);

                switch (result) {
                    case SUCCESS -> {
                        log.info("✅ 발급 성공: couponId={}, userId={}", couponId, userId);
                        couponIssueProducer.send("coupon.issue", String.valueOf(couponId),
                                new CouponIssueEventDto(couponId, userId),
                                ex -> {
                                    // DB에 기록될 길이 끊겼으므로 Redis 발급도 취소해 재고 유령 소모를 막는다.
                                    boolean rolledBack = redisService.rollbackIssue(couponId, userId);
                                    log.error("↩️ Kafka 발행 실패로 발급 롤백: couponId={}, userId={}, rolledBack={}", couponId, userId, rolledBack);
                                });
                    }
                    case OUT_OF_STOCK -> log.info("🎯 재고 소진: couponId={}", couponId);
                    case ALREADY_ISSUED -> log.warn("🚫 중복 발급 시도: couponId={}, userId={}", couponId, userId);
                    default -> log.error("❌ 예기치 않은 결과: {} for couponId={} userId={}", result, couponId, userId);
                }
            }
        }
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
