package com.example.fastcoupon.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {
    CHICKEN("치킨"),
    PIZZA("피자"),
    BURGER("햄버거");

    private final String name;

    CouponTypeEnum(String name) {
        this.name = name;
    }
}
