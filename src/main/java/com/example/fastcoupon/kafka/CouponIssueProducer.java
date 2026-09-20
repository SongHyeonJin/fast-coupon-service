package com.example.fastcoupon.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void send(String topic, String key, T payload) {
        send(topic, key, payload, ex -> {});
    }

    // 전송이 실패하면(브로커 다운, 타임아웃 등) onFailure로 알려 호출자가 보상 처리를 할 수 있게 한다.
    public <T> void send(String topic, String key, T payload, Consumer<Throwable> onFailure) {
        try {
            kafkaTemplate.send(topic, key, payload).toCompletableFuture().whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka 전송 실패: topic={}, key={}, payload={}, error={}", topic, key, payload, ex.getMessage());
                    onFailure.accept(ex);
                } else {
                    log.info("[Kafka] 쿠폰 발급 전송: topic={}, key={}, payload={}", topic, key, payload);
                }
            });
        } catch (Exception e) { // send()가 동기적으로 던지는 경우(metadata 타임아웃, 직렬화 오류)
            log.error("Kafka 전송 예외: topic={}, key={}, payload={}", topic, key, payload, e);
            onFailure.accept(e);
        }
    }

}


