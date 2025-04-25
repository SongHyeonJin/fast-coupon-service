package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.entity.CouponIssue;
import com.example.fastcoupon.enums.ExceptionEnum;
import com.example.fastcoupon.exception.ErrorException;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRepository couponRepository;

    @KafkaListener(topics = "coupon.issue", groupId = "coupon-consumer-group", concurrency = "1")
    public void consume(CouponIssueEventDto event) {
        log.info("[Kafka] 쿠폰 발급 이벤트 수신 : couponId={}, userId={}", event.getCouponId(), event.getUserId());

        try {
            saveCouponIssue(event);
        } catch (DataIntegrityViolationException e) {
            log.info("중복 insert 무시: couponId={}, userId={}", event.getCouponId(), event.getUserId());
        } catch (Exception e) {
            log.error("❌ Kafka 처리 중 예외 발생 - 재시도됨", e);
            throw e;
        }
    }

    @Transactional
    public void saveCouponIssue(CouponIssueEventDto event) {
        Coupon coupon = couponRepository.findById(event.getCouponId()).orElseThrow(
                () -> new ErrorException(ExceptionEnum.COUPON_NOT_FOUND)
        );

        CouponIssue issue = CouponIssue.builder()
                .couponId(event.getCouponId())
                .userId(event.getUserId())
                .build();

        couponIssueRepository.save(issue);
        coupon.decreaseRemainingQuantity();
        couponRepository.save(coupon);
    }

}
