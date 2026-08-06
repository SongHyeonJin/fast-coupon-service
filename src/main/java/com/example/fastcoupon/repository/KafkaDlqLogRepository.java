package com.example.fastcoupon.repository;

import com.example.fastcoupon.entity.KafkaDlqLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaDlqLogRepository extends JpaRepository<KafkaDlqLog, Long> {
}
