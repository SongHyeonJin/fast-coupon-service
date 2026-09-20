package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.entity.Coupon;
import com.example.fastcoupon.enums.CouponTypeEnum;
import com.example.fastcoupon.repository.CouponIssueRepository;
import com.example.fastcoupon.repository.CouponRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueProducerTest {
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    StringRedisTemplate redisTemplate;

    private Long couponId;

    @BeforeEach
    void setup() {
        // ID를 하드코딩하지 않는다. 컨텍스트 생성 순서에 따라 AUTO_INCREMENT가 달라진다.
        couponId = couponRepository.save(Coupon.createCoupon("테스트 쿠폰", CouponTypeEnum.CHICKEN, 100, LocalDateTime.now().plusDays(4))).getId();

        redisTemplate.delete("coupon:" + couponId + ":count");
        Set<String> keys = redisTemplate.keys("coupon:" + couponId + ":user:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        couponIssueRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        couponIssueRepository.deleteAllInBatch();
        couponRepository.deleteAllInBatch();
    }

    @DisplayName("Kafka 발급 이벤트 전송 시 Consumer가 DB에 저장한다")
    @Test
    void Kafka_이벤트_수신_후_DB_저장_검증() throws Exception {
        // given
        Long userId = 777L;
        CouponIssueEventDto eventDto = new CouponIssueEventDto(couponId, userId);

        // when
        kafkaTemplate.send("coupon.issue", eventDto).get();
        System.out.println("✅ Kafka 메시지 전송 완료!");

        // then
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var result = couponIssueRepository.findAll();
                    assertThat(result).hasSize(1);
                    assertThat(result.get(0).getUserId()).isEqualTo(userId);
                });
    }
}