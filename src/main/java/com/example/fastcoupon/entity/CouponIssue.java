package com.example.fastcoupon.entity;

import com.example.fastcoupon.entity.base.Timestamped;
import com.example.fastcoupon.enums.CouponStatusEnum;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issue", uniqueConstraints = {
        @UniqueConstraint(name = "uk_coupon_user", columnNames = {"coupon_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 사용 여부
    @Column(name = "used", nullable = false)
    private boolean used = false;

    // 사용 일시
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // 상태 (UNUSED, USED, EXPIRED 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatusEnum status = CouponStatusEnum.UNUSED;

    @Builder
    public CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
    }

    // 쿠폰 사용 처리
    public void use() {
        if (this.used) {
            throw new ErrorException(ExceptionEnum.COUPON_ALREADY_USED);
        }
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.status = CouponStatusEnum.USED;
    }
}
