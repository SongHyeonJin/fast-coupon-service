package com.example.fastcoupon.entity;

import com.example.fastcoupon.entity.base.Timestamped;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KafkaDlqLog extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topicName;

    @Lob
    private String keyValue;

    @Lob
    private String messageValue;

    @Lob
    private String headers;

    private String reason;

    private Integer retryCount;

    private Long couponId;

    private Long userId;

    @Builder
    public KafkaDlqLog(String topicName, String keyValue, String messageValue, String headers,
                       String reason, Integer retryCount, Long couponId, Long userId) {
        this.topicName = topicName;
        this.keyValue = keyValue;
        this.messageValue = messageValue;
        this.headers = headers;
        this.reason = reason;
        this.retryCount = retryCount;
        this.couponId = couponId;
        this.userId = userId;
    }
}
