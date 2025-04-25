package com.example.fastcoupon.repository;

import com.example.fastcoupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByExpiredAtBetween(LocalDateTime from, LocalDateTime now);

}
