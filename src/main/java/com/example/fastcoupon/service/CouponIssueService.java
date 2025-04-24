package com.example.fastcoupon.service;

import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.kafka.CouponIssueProducer;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponRepository couponRepository;
    private final CouponIssueProducer couponIssueProducer;

    public void sendIssueEvent(Long couponId, Long userId) {

        couponRepository.findById(couponId).orElseThrow(
                () -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND)
        );

        couponIssueProducer.sendIssueEvent(couponId, userId);
    }

}
