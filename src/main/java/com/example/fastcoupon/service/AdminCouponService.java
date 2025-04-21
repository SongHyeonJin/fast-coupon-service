package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.coupon.CouponRequestDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponRepository;
import com.example.fastcoupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public void createCoupon(CouponRequestDto requestDto, User user) {
        userRepository.findByEmail(user.getEmail()).orElseThrow(
                () -> new ErrorException(ExceptionEnum.USER_NOT_FOUND)
        );


        Coupon coupon = Coupon.createCoupon(
                requestDto.getName(),
                requestDto.getType(),
                requestDto.getTotalQuantity(),
                requestDto.getExpiredAt()
        );
        couponRepository.save(coupon);
    }

}
