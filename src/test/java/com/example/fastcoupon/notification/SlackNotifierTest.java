package com.example.fastcoupon.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "slack.webhook.url=http://127.0.0.1:1/invalid")
class SlackNotifierTest {

    @Autowired
    SlackNotifier slackNotifier;

    @DisplayName("Slack 전송이 실패해도 예외가 밖으로 전파되지 않는다")
    @Test
    void notifyDlqFailure_전송실패해도_예외를_삼킨다() {
        assertThatCode(() -> slackNotifier.notifyDlqFailure(1L, 2L, "테스트 실패 사유"))
                .doesNotThrowAnyException();
    }
}
