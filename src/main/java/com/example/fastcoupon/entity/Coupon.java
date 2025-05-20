package com.example.fastcoupon.entity;

import com.example.fastcoupon.entity.base.Timestamped;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends Timestamped {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTypeEnum type;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int remainingQuantity;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiredAt;

    public static Coupon createCoupon(String name, CouponTypeEnum type, int totalQuantity, LocalDateTime expiredAt) {
        Coupon coupon = new Coupon();
        coupon.name = name;
        coupon.type = type;
        coupon.totalQuantity = totalQuantity;
        coupon.remainingQuantity = totalQuantity;
        coupon.expiredAt = expiredAt;
        return coupon;
    }

    public void decreaseRemainingQuantity() {
        if (this.remainingQuantity == 0) return;

        if (this.remainingQuantity > 0) {
            this.remainingQuantity -= 1;
        } else {
            throw new ErrorException(ExceptionEnum.COUPON_OUT_OF_STOCK);
        }
    }

}
