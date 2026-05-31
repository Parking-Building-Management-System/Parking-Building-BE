package com.smartpark.swp391.modules.payment.repository;

import com.smartpark.swp391.modules.payment.entity.PaymentWebhookLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, UUID> {}
