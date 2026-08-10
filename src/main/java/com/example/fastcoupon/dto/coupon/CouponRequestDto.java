package com.example.fastcoupon.dto.coupon;

import com.example.fastcoupon.enums.CouponTypeEnum;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CouponRequestDto {

    @NotBlank(message = "쿠폰 이름을 입력해주세요.")
    private String name;

    @NotNull(message = "쿠폰 타입을 선택해주세요.")
    private CouponTypeEnum type;

    @Positive(message = "발급 수량은 1개 이상이어야 합니다.")
    private int totalQuantity;

    @NotNull(message = "만료일시를 입력해주세요.")
    @Future(message = "만료일시는 현재 시각 이후여야 합니다.")
    private LocalDateTime expiredAt;

}
