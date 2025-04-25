package com.example.fastcoupon.scheduler;

import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.CouponStatusEnum;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireScheduler {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Scheduled(cron = "0 0 * * * *")
//    @Scheduled(cron = "*/5 * * * * *")
    @Transactional
    public void markExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        List<Coupon> expiredCoupons = couponRepository.findAllByExpiredAtBetween(oneHourAgo, now);

        for (Coupon coupon : expiredCoupons) {
            List<CouponIssue> issues = couponIssueRepository.findAllByCouponIdAndStatus(
                    coupon.getId(), CouponStatusEnum.UNUSED
            );
            for (CouponIssue issue : issues) {
                issue.updateStatus(CouponStatusEnum.EXPIRED);
            }

            log.info("✅ [{}~{}] 쿠폰 ID={}의 미사용 발급건 {}건 만료 처리 완료",
                    oneHourAgo, now, coupon.getId(), issues.size());
        }
    }
}