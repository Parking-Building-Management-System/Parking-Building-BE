package com.smartpark.swp391.modules.payment.entity;

import com.smartpark.swp391.infrastructure.persistence.DomainBaseEntity;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.payment.enumType.PaymentWebhookLogStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "payment_webhook_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookLog extends DomainBaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private PaymentProvider provider = PaymentProvider.PAYOS;

  @Column(name = "event_code", length = 100)
  private String eventCode;

  @Column(name = "order_code")
  private Long orderCode;

  @Column(columnDefinition = "text")
  private String signature;

  @Column(name = "payload_json", nullable = false, columnDefinition = "text")
  private String payloadJson;

  @Column(name = "received_at", nullable = false)
  @Builder.Default
  private LocalDateTime receivedAt = LocalDateTime.now();

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private PaymentWebhookLogStatus status = PaymentWebhookLogStatus.RECEIVED;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;
}
