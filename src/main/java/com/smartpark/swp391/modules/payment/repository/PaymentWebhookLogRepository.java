package com.smartpark.swp391.modules.payment.repository;

import com.smartpark.swp391.modules.payment.entity.PaymentWebhookLog;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, UUID> {

  @Modifying
  @Query(
      value = "DELETE FROM payment_webhook_logs WHERE order_code IN :orderCodes",
      nativeQuery = true)
  int deleteByOrderCodes(@Param("orderCodes") Collection<Long> orderCodes);
}
