package com.example.fastcoupon.controller;

import com.example.fastcoupon.dto.common.BasicResponseDto;
import com.example.fastcoupon.dto.coupon.CouponRequestDto;
import com.example.fastcoupon.security.UserDetailsImpl;
import com.example.fastcoupon.service.AdminCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @PostMapping("/coupons")
    public ResponseEntity<BasicResponseDto> createCoupon(@RequestBody CouponRequestDto requestDto,
                                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adminCouponService.createCoupon(requestDto, userDetails.getUser());
        return ResponseEntity.ok(BasicResponseDto.addSuccess("쿠폰 등록 완료"));
    }

}
