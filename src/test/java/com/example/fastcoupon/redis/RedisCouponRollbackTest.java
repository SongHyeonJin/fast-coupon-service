package com.example.fastcoupon.redis;

import com.example.fastcoupon.enums.CouponIssueEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RedisCouponRollbackTest {

    private static final long COUPON_ID = 987654L;
    private static final long USER_ID = 1L;

    @Autowired RedisCouponService redisCouponService;
    @Autowired StringRedisTemplate redisTemplate;

    @BeforeEach
    void setup() {
        // tryIssueCoupon이 expire 키의 TTL을 유저 키 TTL로 쓴다 (실제로는 쿠폰 생성 시 세팅됨)
        redisTemplate.opsForValue().set("coupon:987654:expire", "", Duration.ofMinutes(10));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(Arrays.asList("coupon:987654:expire", "coupon:987654:count", "coupon:987654:user:1"));
    }

    @DisplayName("Kafka 발행 실패 시 롤백하면 재고와 유저 발급 기록이 원복되어 재발급이 가능하다")
    @Test
    void rollbackRestoresStockAndUser() {
        assertThat(redisCouponService.tryIssueCoupon(COUPON_ID, USER_ID, 1)).isEqualTo(CouponIssueEnum.SUCCESS);
        assertThat(redisCouponService.tryIssueCoupon(COUPON_ID, 2L, 1)).isEqualTo(CouponIssueEnum.OUT_OF_STOCK);

        assertThat(redisCouponService.rollbackIssue(COUPON_ID, USER_ID)).isTrue();
        assertThat(redisCouponService.getCurrentCount(COUPON_ID)).isZero();

        // 멱등: 두 번째 롤백은 아무것도 하지 않아 count가 음수가 되지 않는다
        assertThat(redisCouponService.rollbackIssue(COUPON_ID, USER_ID)).isFalse();
        assertThat(redisCouponService.getCurrentCount(COUPON_ID)).isZero();

        assertThat(redisCouponService.tryIssueCoupon(COUPON_ID, USER_ID, 1)).isEqualTo(CouponIssueEnum.SUCCESS);
    }
}
