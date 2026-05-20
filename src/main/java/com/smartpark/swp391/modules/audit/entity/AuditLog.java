package com.smartpark.swp391.modules.audit.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class AuditLog extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id")
  private User actorUser;

  @Column(nullable = false, length = 100)
  private String action;

  @Column(name = "resource_type", nullable = false, length = 100)
  private String resourceType;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(columnDefinition = "text")
  private String metadata;

  @Column(name = "occurred_at", nullable = false)
  @Builder.Default
  private LocalDateTime occurredAt = LocalDateTime.now();
}
