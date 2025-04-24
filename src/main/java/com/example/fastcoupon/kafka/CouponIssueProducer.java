package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final KafkaTemplate<String, CouponIssueEventDto> kafkaTemplate;
    private static final String TOPIC = "coupon.issue";

    public void sendIssueEvent(Long couponId, Long userId)  {
        CouponIssueEventDto event = new CouponIssueEventDto(couponId, userId);

        kafkaTemplate.send(TOPIC, couponId.toString(), event)
                .toCompletableFuture().whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 메시지 전송 실패: couponId={}, userId={}, error={}", couponId, userId, ex.getMessage());
                    } else {
                        log.info("[Kafka] 쿠폰 발급 이벤트 전송 : couponId={}, userId={}", couponId, userId);
                    }
                });
    }

}
