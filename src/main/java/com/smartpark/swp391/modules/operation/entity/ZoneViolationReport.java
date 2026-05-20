package com.smartpark.swp391.modules.operation.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.enumType.ViolationReportStatus;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "zone_violation_report")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class ZoneViolationReport extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_session_id", nullable = false)
  private ParkingSession parkingSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "zone_id", nullable = false)
  private Zone zone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "slot_id", nullable = false)
  private Slot slot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_by")
  private User reportedBy;

  @Column(name = "violation_type", nullable = false, length = 100)
  private String violationType;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ViolationReportStatus status = ViolationReportStatus.OPEN;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;
}
