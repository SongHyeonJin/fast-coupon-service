package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueSaveService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRepository couponRepository;

    // REQUIRED(기본값): 이 메서드 자체가 트랜잭션의 시작점이라 REQUIRES_NEW로 얻을 이점이 없고,
    // 커넥션만 하나 더 점유하게 된다. 발급 저장과 재고 차감이 하나의 트랜잭션으로 묶여야
    // uk_coupon_user 중복 발급 시 재고 차감 없이 통째로 롤백된다.
    @Transactional
    public void saveCouponIssue(CouponIssueEventDto event) {
        Coupon coupon = couponRepository.findById(event.getCouponId()).orElseThrow(
                () -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND)
        );

        CouponIssue issue = CouponIssue.builder()
                .couponId(event.getCouponId())
                .userId(event.getUserId())
                .build();

        couponIssueRepository.save(issue);
        coupon.decreaseRemainingQuantity();
        couponRepository.save(coupon);
    }

}
