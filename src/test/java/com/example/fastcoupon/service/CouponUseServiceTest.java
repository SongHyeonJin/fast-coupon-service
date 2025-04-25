package com.example.fastcoupon.service;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.CouponStatusEnum;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponUseServiceTest {

    @Autowired
    CouponUseService couponUseService;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    private Long userId = 123L;
    private Coupon coupon;
    private CouponIssue couponIssue;

    @BeforeEach
    void setup() {
        coupon = couponRepository.save(
                Coupon.createCoupon("테스트 쿠폰", CouponTypeEnum.CHICKEN, 10, LocalDateTime.now().plusDays(1))
        );
        couponIssue = couponIssueRepository.save(
                CouponIssue.builder()
                        .couponId(coupon.getId())
                        .userId(userId)
                        .build()
        );
    }

    @AfterEach
    void tearDown() {
        couponRepository.deleteAllInBatch();
        couponIssueRepository.deleteAllInBatch();
    }

    @DisplayName("정상적인 쿠폰 사용 요청은 성공해야 한다")
    @Test
    void useCoupon_성공() {
        // given
        Long couponId = coupon.getId();
        Long couponIssueId = couponIssue.getId();

        // when
        couponUseService.useCoupon(couponId, couponIssueId, userId);

        // then

        CouponIssue result = couponIssueRepository.findById(couponIssue.getId()).get();
        assertThat(result.isUsed()).isTrue();
        assertThat(result.getStatus()).isEqualTo(CouponStatusEnum.USED);
        assertThat(result.getUsedAt()).isNotNull();
    }

    @DisplayName("다른 유저가 사용하려고 하면 예외가 발생한다")
    @Test
    void useCoupon_다른유저_예외() {
        // given
        Long otherUser = 999L;

        // when & then
        assertThatThrownBy(() ->
                couponUseService.useCoupon(coupon.getId(), couponIssue.getId(), otherUser)
        ).isInstanceOf(ErrorException.class);
    }

    @DisplayName("이미 사용한 쿠폰은 다시 사용할 수 없다")
    @Test
    void useCoupon_이미사용된쿠폰_예외() {
        // given
        couponIssue.updateStatus(CouponStatusEnum.USED);
        couponIssue.updateUsed(true);
        couponIssueRepository.save(couponIssue);

        // when & then
        assertThatThrownBy(() ->
                couponUseService.useCoupon(coupon.getId(), couponIssue.getId(), userId)
        ).isInstanceOf(ErrorException.class);
    }

    @DisplayName("유효기간이 지난 쿠폰은 사용할 수 없다")
    @Test
    void useCoupon_만료된쿠폰_예외() {
        // given
        Coupon expiredCoupon = couponRepository.save(
                Coupon.createCoupon("만료 쿠폰", CouponTypeEnum.CHICKEN, 10, LocalDateTime.now().minusDays(1))
        );
        CouponIssue expiredIssue = couponIssueRepository.save(
                CouponIssue.builder()
                        .couponId(expiredCoupon.getId())
                        .userId(userId)
                        .build()
        );

        // when & then
        assertThatThrownBy(() ->
                couponUseService.useCoupon(expiredCoupon.getId(), expiredIssue.getId(), userId)
        ).isInstanceOf(ErrorException.class);
    }
}
