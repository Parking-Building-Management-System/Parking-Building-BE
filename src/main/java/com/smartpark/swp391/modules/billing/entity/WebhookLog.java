package com.smartpark.swp391.modules.billing.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.billing.enumType.WebhookStatus;
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
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "webhook_log")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class WebhookLog extends TenantScopedEntity {

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "external_id", length = 255)
  private String externalId;

  @Column(columnDefinition = "text")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private WebhookStatus status = WebhookStatus.RECEIVED;

  @Column(name = "received_at", nullable = false)
  @Builder.Default
  private LocalDateTime receivedAt = LocalDateTime.now();

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;
}
