package com.smartpark.swp391.modules.operation.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "kiosk_staff")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class KioskStaff extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "kiosk_id", nullable = false)
  private Kiosk kiosk;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_user_id", nullable = false)
  private User staffUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shift_id")
  private Shift shift;

  @Column(name = "assigned_at", nullable = false)
  @Builder.Default
  private LocalDateTime assignedAt = LocalDateTime.now();

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;
}
