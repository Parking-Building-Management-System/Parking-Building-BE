package com.smartpark.swp391.modules.vehicle.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_vehicle_link")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class UserVehicleLink extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @Column(name = "license_plate", nullable = false, length = 30)
  private String licensePlate;

  @Column(name = "vehicle_label", length = 100)
  private String vehicleLabel;

  @Column(name = "is_default", nullable = false)
  @Builder.Default
  private boolean defaultVehicle = false;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
