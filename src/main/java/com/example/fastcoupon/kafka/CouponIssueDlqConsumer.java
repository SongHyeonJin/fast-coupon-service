package com.example.fastcoupon.kafka;

import com.example.fastcoupon.dto.coupon.CouponIssueEventDto;
import com.example.fastcoupon.enums.ExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CouponIssueDlqConsumer {

    @KafkaListener(topics = "coupon.issue.dlq", groupId = "coupon-dlq-group",
            concurrency = "1", containerFactory = "dlqKafkaListenerContainerFactory")
    public void handleDLQ(CouponIssueEventDto event) {
        String reasonMsg = event.getReason() != null
                ? event.getReason()
                : ExceptionEnum.DB_SAVE_FAILED.getMsg();

        log.warn("💀 [DLQ] 쿠폰 발급 실패 - couponId={}, userId={}, reason={}",
                event.getCouponId(), event.getUserId(), reasonMsg);
    }

}
