package com.example.fastcoupon.service;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.CouponStatusEnum;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponUseService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void useCoupon(Long couponId, Long couponIssueId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND));

        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId).orElseThrow(
                () -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND)
        );

        if (!couponIssue.getUserId().equals(userId)) {
            throw new ErrorException(ExceptionEnum.NOT_ALLOW);
        }

        if (couponIssue.isUsed() || couponIssue.getStatus().equals(CouponStatusEnum.USED)) {
            throw new ErrorException(ExceptionEnum.COUPON_ALREADY_USED);
        }

        if (couponIssue.getStatus().equals(CouponStatusEnum.EXPIRED)) {
            throw new ErrorException(ExceptionEnum.COUPON_EXPIRED);
        }

        if (coupon.getExpiredAt().isBefore(LocalDateTime.now())) {
            couponIssue.updateStatus(CouponStatusEnum.EXPIRED);
            log.info("⛔ 만료된 쿠폰 사용 시도 차단 - couponId={}, userId={}", couponId, userId);
            throw new ErrorException(ExceptionEnum.COUPON_EXPIRED);
        }

        couponIssue.updateUsed(true);
        couponIssue.updateUsedAt(LocalDateTime.now());
        couponIssue.updateStatus(CouponStatusEnum.USED);
    }
}
