package com.smartpark.swp391.modules.parking.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.parking.enumType.ZoneStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "zones")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class Zone extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(name = "floor_name", length = 50)
  private String floorName;

  @Column(nullable = false)
  @Builder.Default
  private Integer capacity = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ZoneStatus status = ZoneStatus.ACTIVE;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
}
