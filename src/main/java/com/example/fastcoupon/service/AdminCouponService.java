package com.example.fastcoupon.service;

import com.example.fastcoupon.dto.coupon.CouponRequestDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.User;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponRepository;
import com.example.fastcoupon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final StringRedisTemplate redisTemplate;

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

        redisTemplate.opsForSet().add("coupon:active:ids", coupon.getId().toString());

        String totalKey = String.format("coupon:%d:total", coupon.getId());
        redisTemplate.opsForValue()
                .set(totalKey, String.valueOf(coupon.getTotalQuantity()));

        String expireKey = String.format("coupon:%d:expire", coupon.getId());
        long ttlSeconds = Duration.between(LocalDateTime.now(), coupon.getExpiredAt()).getSeconds();
        redisTemplate.opsForValue()
                .set(expireKey, "", Duration.ofSeconds(Math.max(ttlSeconds, 0)));
    }

}
