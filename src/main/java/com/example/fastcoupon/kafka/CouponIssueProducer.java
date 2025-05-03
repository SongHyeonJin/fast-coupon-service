package com.example.fastcoupon.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void send(String topic, String key, T payload) {
        kafkaTemplate.send(topic, key, payload)
                .toCompletableFuture()
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka 전송 실패: topic={}, key={}, payload={}, error={}", topic, key, payload, ex.getMessage());
                    } else {
                        log.info("[Kafka] 쿠폰 발급 전송: topic={}, key={}, payload={}", topic, key, payload);
                    }
                });
    }

}


