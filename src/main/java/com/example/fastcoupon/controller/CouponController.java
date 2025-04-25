package com.example.fastcoupon.controller;

import com.example.fastcoupon.dto.common.BasicResponseDto;
import com.example.fastcoupon.redis.RedisCouponService;
import com.example.fastcoupon.security.UserDetailsImpl;
import com.example.fastcoupon.service.CouponUseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final RedisCouponService redisCouponService;
    private final CouponUseService couponUseService;

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<BasicResponseDto> issueCoupon(
            @PathVariable("couponId") Long couponId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
            ) {
        redisCouponService.pushQueue(couponId, userDetails.getUser().getId());
        return ResponseEntity.ok(BasicResponseDto.addSuccess("쿠폰 발급 성공"));
    }

    @PostMapping("/{couponId}/issued/{couponIssueId}/use")
    public ResponseEntity<BasicResponseDto> useCoupon(
            @PathVariable("couponId") Long couponId,
            @PathVariable("couponIssueId") Long couponIssueId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        couponUseService.useCoupon(couponId, couponIssueId, userDetails.getUser().getId());
        return ResponseEntity.ok(BasicResponseDto.addSuccess("쿠폰 사용 완료"));
    }

}
