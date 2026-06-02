package com.smartpark.swp391.modules.firesafety.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "fire_extinguishers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class FireExtinguisher extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "floor_id", nullable = false)
  private Floor floor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "zone_id")
  private Zone zone;

  @Column(nullable = false, length = 100)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FireExtinguisherType type;

  @Column(name = "location_description", columnDefinition = "text")
  private String locationDescription;

  @Column(name = "x_coordinate", precision = 5, scale = 2)
  private BigDecimal xCoordinate;

  @Column(name = "y_coordinate", precision = 5, scale = 2)
  private BigDecimal yCoordinate;

  @Column(name = "manufacture_date")
  private LocalDate manufactureDate;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(name = "last_inspected_at")
  private LocalDateTime lastInspectedAt;

  @Column(name = "next_inspection_at")
  private LocalDateTime nextInspectionAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private FireExtinguisherStatus status = FireExtinguisherStatus.ACTIVE;

  @Column(columnDefinition = "text")
  private String note;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
