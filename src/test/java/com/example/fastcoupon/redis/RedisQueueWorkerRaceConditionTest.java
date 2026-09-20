package com.example.fastcoupon.redis;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisQueueWorker의 락 해제 타이밍 레이스를 재현한다.
 * 워커가 이미 락(coupon:{id}:running)을 쥐고 있는 상태에서 새 요청이 들어오면
 * CouponQueueEventDto가 드롭되어 아이템만 큐에 남는다. 그 직후 (실제로는 원래
 * 워커가 자기 작업을 끝내고) 락이 풀리는 상황을 재현했을 때, 아무도 다시
 * 깨워주지 않으면 그 아이템은 영원히 처리되지 않는다 — 이 테스트는 그 버그를
 * 고정된 재현 시나리오로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisQueueWorkerRaceConditionTest {

    @Autowired
    RedisCouponService redisCouponService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    private Coupon coupon;

    @BeforeEach
    void setup() {
        coupon = couponRepository.save(
                Coupon.createCoupon("테스트 쿠폰", CouponTypeEnum.CHICKEN, 10, LocalDateTime.now().plusDays(1))
        );
        deleteCouponKeys(); // 다른 테스트가 같은 ID로 남긴 done/running 키 제거
        redisTemplate.opsForValue().set("coupon:" + coupon.getId() + ":total", "10");
        redisTemplate.opsForValue().set("coupon:" + coupon.getId() + ":expire", "", Duration.ofMinutes(10));
        // RedisQueueWorker.sweepStuckQueues()가 이 세트를 스캔 대상으로 삼는다
        // (AdminCouponService.createCoupon()이 실제로 하는 것과 동일하게 등록).
        redisTemplate.opsForSet().add("coupon:active:ids", String.valueOf(coupon.getId()));
    }

    @AfterEach
    void tearDown() {
        couponIssueRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();

        redisTemplate.opsForSet().remove("coupon:active:ids", String.valueOf(coupon.getId()));
        deleteCouponKeys();
    }

    private void deleteCouponKeys() {
        Set<String> keys = redisTemplate.keys("coupon:" + coupon.getId() + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @DisplayName("워커가 락을 쥐고 있는 동안 들어온 요청은 락이 풀린 뒤 자동으로 처리돼야 한다")
    @Test
    void 락이_걸린_동안_버려진_요청도_결국_발급된다() {
        long couponId = coupon.getId();
        String lockKey = "coupon:" + couponId + ":running";
        String queueKey = "coupon:" + couponId + ":queue";

        // given: 워커가 이미 실행 중인 것처럼 락을 미리 잡아둔다
        redisTemplate.opsForValue().set(lockKey, "true", Duration.ofMinutes(10));

        // when: 이 상태에서 새 요청이 들어오면 이벤트가 락 때문에 드롭되고 큐에만 쌓인다
        redisCouponService.pushQueue(couponId, 1L);
        assertThat(redisTemplate.opsForList().size(queueKey)).isEqualTo(1L);

        // 실제 워커가 이 시점에 자기 작업을 끝내고 락을 반납하는 상황을 재현
        redisTemplate.delete(lockKey);

        // then: 아무도 다시 깨워주지 않으면(현재 버그) 이 아이템은 처리되지 않는다
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(couponIssueRepository.findAll()).hasSize(1));
    }
}
