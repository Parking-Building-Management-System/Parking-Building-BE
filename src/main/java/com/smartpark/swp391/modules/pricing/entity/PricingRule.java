package com.smartpark.swp391.modules.pricing.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class PricingRule extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id")
  private Parking parking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "free_minutes", nullable = false)
  @Builder.Default
  private Integer freeMinutes = 0;

  @Column(name = "first_block_minutes", nullable = false)
  private Integer firstBlockMinutes;

  @Column(name = "first_block_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal firstBlockPrice;

  @Column(name = "next_block_minutes", nullable = false)
  private Integer nextBlockMinutes;

  @Column(name = "next_block_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal nextBlockPrice;

  @Column(name = "daily_cap_price", precision = 12, scale = 2)
  private BigDecimal dailyCapPrice;

  @Column(name = "grace_minutes_after_payment", nullable = false)
  @Builder.Default
  private Integer graceMinutesAfterPayment = 15;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private PricingRuleStatus status = PricingRuleStatus.ACTIVE;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
