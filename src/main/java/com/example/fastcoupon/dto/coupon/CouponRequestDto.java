package com.example.fastcoupon.dto.coupon;

import com.example.fastcoupon.enums.CouponTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponRequestDto {

    private String name;
    private CouponTypeEnum type;
    private int totalQuantity;
    private LocalDateTime expiredAt;

}
