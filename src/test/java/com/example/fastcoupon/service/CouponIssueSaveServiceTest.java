package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueSaveServiceTest {

    @Autowired
    CouponIssueSaveService couponIssueSaveService;

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
    }

    @AfterEach
    void tearDown() {
        couponIssueRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    @DisplayName("발급 저장과 재고 차감은 한 트랜잭션으로 함께 커밋된다")
    @Test
    void saveCouponIssue_발급저장과_재고차감이_함께_반영된다() {
        // when
        couponIssueSaveService.saveCouponIssue(new CouponIssueEventDto(coupon.getId(), 1L));

        // then
        assertThat(couponIssueRepository.findAll()).hasSize(1);
        Coupon result = couponRepository.findById(coupon.getId()).get();
        assertThat(result.getRemainingQuantity()).isEqualTo(9);
    }

    @DisplayName("동일 유저의 중복 발급 메시지는 uk_coupon_user 위반으로 롤백되고, 재고는 과차감되지 않는다")
    @Test
    void saveCouponIssue_중복메시지는_재고를_과차감하지_않는다() {
        // given: 최초 발급 성공
        CouponIssueEventDto event = new CouponIssueEventDto(coupon.getId(), 1L);
        couponIssueSaveService.saveCouponIssue(event);

        // when: 같은 이벤트가 재전달(중복)됨 - 자기 호출이 아닌 빈 호출이라 트랜잭션이 통째로 롤백돼야 함
        assertThatThrownBy(() -> couponIssueSaveService.saveCouponIssue(event))
                .isInstanceOf(DataIntegrityViolationException.class);

        // then: 발급 기록은 1건, 재고는 최초 1건 차감분(9)에서 더 깎이지 않아야 함
        assertThat(couponIssueRepository.findAll()).hasSize(1);
        Coupon result = couponRepository.findById(coupon.getId()).get();
        assertThat(result.getRemainingQuantity()).isEqualTo(9);
    }
}
