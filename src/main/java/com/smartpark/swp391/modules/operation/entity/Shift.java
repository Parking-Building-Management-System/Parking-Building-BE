package com.smartpark.swp391.modules.operation.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.parking.entity.Parking;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "shift")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class Shift extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;
}
