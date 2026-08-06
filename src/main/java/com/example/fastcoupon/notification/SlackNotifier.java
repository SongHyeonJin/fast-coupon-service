package com.example.fastcoupon.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class SlackNotifier {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public void notifyDlqFailure(Long couponId, Long userId, String reason) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("slack.webhook.url 미설정 - DLQ 알림 스킵");
            return;
        }

        try {
            String text = "🚨 쿠폰 발급 DLQ 유입\ncouponId=%s, userId=%s\nreason=%s"
                    .formatted(couponId, userId, reason);
            restTemplate.postForEntity(webhookUrl, Map.of("text", text), String.class);
        } catch (Exception e) {
            // Slack 전송 실패가 DLQ 컨슈머를 죽이면 안 되므로 로그만 남기고 삼킨다.
            log.error("Slack DLQ 알림 전송 실패: couponId={}, userId={}", couponId, userId, e);
        }
    }
}
