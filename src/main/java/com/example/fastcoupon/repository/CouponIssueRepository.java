package com.example.fastcoupon.repository;

import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.CouponStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    List<CouponIssue> findAllByCouponIdAndStatus(Long couponId, CouponStatusEnum status);

}
