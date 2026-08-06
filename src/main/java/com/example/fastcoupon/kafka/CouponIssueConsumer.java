package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.service.CouponIssueSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueSaveService couponIssueSaveService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            dltTopicSuffix = ".dlq",
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "coupon.issue", groupId = "coupon-consumer-group", concurrency = "1")
    public void consume(CouponIssueEventDto event) {
        log.info("[Kafka] 쿠폰 발급 이벤트 수신 : couponId={}, userId={}", event.getCouponId(), event.getUserId());

        try {
            // 별도 빈(couponIssueSaveService) 호출이라 프록시를 타고 @Transactional이 정상 적용됨
            couponIssueSaveService.saveCouponIssue(event);
        } catch (DataIntegrityViolationException e) {
            log.info("중복 insert 무시: couponId={}, userId={}", event.getCouponId(), event.getUserId());
        } catch (Exception e) {
            log.error("❌ Kafka 처리 중 예외 발생 - 재시도됨", e);
            throw e;
        }
    }

}
