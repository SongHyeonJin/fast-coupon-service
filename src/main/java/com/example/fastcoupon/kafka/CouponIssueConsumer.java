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
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRepository couponRepository;

    @KafkaListener(topics = "coupon.issue", groupId = "coupon-consumer-group",
            concurrency = "1", containerFactory = "kafkaListenerContainerFactory")
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            dltTopicSuffix = ".dlq", // 실패 시 자동으로 coupon.issue.dlq 로 전송
            autoCreateTopics = "false" // 필요 시 true로 설정
    )
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


        coupon.decreaseRemainingQuantity();
        couponRepository.save(coupon);
        couponIssueRepository.save(issue);
    }

}
