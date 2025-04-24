package com.example.fastcoupon.dto.coupon;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CouponIssueEventDto {

    private Long couponId;
    private Long userId;

    public CouponIssueEventDto(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
    }
}
