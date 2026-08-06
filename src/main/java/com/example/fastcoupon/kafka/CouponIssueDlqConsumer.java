package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.entity.KafkaDlqLog;
import com.example.fastcoupon.notification.SlackNotifier;
import com.example.fastcoupon.repository.KafkaDlqLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueDlqConsumer {

    private final KafkaDlqLogRepository kafkaDlqLogRepository;
    private final SlackNotifier slackNotifier;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "coupon.issue.dlq", groupId = "coupon-dlq-group", concurrency = "1")
    public void handleDLQ(
            CouponIssueEventDto event,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String reason
    ) {
        log.warn("💀 [DLQ] 쿠폰 발급 실패 - couponId={}, userId={}, reason={}",
                event.getCouponId(), event.getUserId(), reason);

        kafkaDlqLogRepository.save(KafkaDlqLog.builder()
                .topicName(originalTopic)
                .messageValue(toJson(event))
                .reason(reason)
                .couponId(event.getCouponId())
                .userId(event.getUserId())
                .build());

        slackNotifier.notifyDlqFailure(event.getCouponId(), event.getUserId(), reason);
    }

    private String toJson(CouponIssueEventDto event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
