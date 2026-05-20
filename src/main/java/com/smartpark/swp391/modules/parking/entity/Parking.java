package com.smartpark.swp391.modules.parking.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "parkings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class Parking extends TenantScopedEntity {

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(columnDefinition = "text")
  private String address;

  @Column(name = "total_capacity", nullable = false)
  @Builder.Default
  private Integer totalCapacity = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ParkingStatus status = ParkingStatus.ACTIVE;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
}
