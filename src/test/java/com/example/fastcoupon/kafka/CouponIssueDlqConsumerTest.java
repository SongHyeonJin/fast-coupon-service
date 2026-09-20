package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.repository.KafkaDlqLogRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueDlqConsumerTest {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    KafkaDlqLogRepository kafkaDlqLogRepository;

    @BeforeEach
    @AfterEach
    void cleanLogs() {
        kafkaDlqLogRepository.deleteAllInBatch();
    }

    @DisplayName("존재하지 않는 쿠폰 이벤트는 3회 재시도 후 DLQ로 빠지고 KafkaDlqLog에 기록된다")
    @Test
    void 재시도_모두_실패하면_DLQ에_기록된다() throws Exception {
        // given: DB에 존재하지 않는 couponId라서 saveCouponIssue가 매번 COUPON_NOT_FOUND로 실패한다
        Long nonExistentCouponId = 999_999_999L;
        Long userId = 555L;
        CouponIssueEventDto event = new CouponIssueEventDto(nonExistentCouponId, userId);

        // when
        kafkaTemplate.send("coupon.issue", event).get();

        // then: attempts=3, backoff 2s/4s + 재시도 토픽 컨슈머 그룹 조인 시간까지 감안해 넉넉히 40초 대기
        Awaitility.await()
                .atMost(Duration.ofSeconds(40))
                .untilAsserted(() -> {
                    // 다른 테스트가 만든 DLQ 로그가 섞일 수 있으므로 이 테스트의 이벤트만 본다
                    var logs = kafkaDlqLogRepository.findAll().stream()
                            .filter(l -> nonExistentCouponId.equals(l.getCouponId()))
                            .toList();
                    assertThat(logs).hasSize(1);
                    assertThat(logs.get(0).getCouponId()).isEqualTo(nonExistentCouponId);
                    assertThat(logs.get(0).getUserId()).isEqualTo(userId);
                    assertThat(logs.get(0).getReason()).isNotBlank();
                });
    }
}
