package com.smartpark.swp391.modules.parking.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "parkings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Parking extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

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
