package com.smartpark.swp391.modules.vehicle.entity;

import com.smartpark.swp391.infrastructure.persistence.DomainBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class VehicleType extends DomainBaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50, unique = true)
  private String code;

  @Column(name = "is_active", nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
