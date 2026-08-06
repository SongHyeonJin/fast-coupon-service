package com.example.fastcoupon.dto.coupon;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CouponQueueEventDto {

    private Long couponId;
    private int totalQuantity;

    public CouponQueueEventDto(Long couponId, int totalQuantity) {
        this.couponId = couponId;
        this.totalQuantity = totalQuantity;
    }
}
