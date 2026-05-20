package com.smartpark.swp391.modules.subscription.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.subscription.enumType.SubscriptionStatus;
import com.smartpark.swp391.modules.vehicle.entity.UserVehicleLink;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class Subscription extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_vehicle_link_id", nullable = false)
  private UserVehicleLink userVehicleLink;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @Column(name = "period_string", nullable = false, length = 20)
  private String periodString;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal monthlyPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private SubscriptionStatus status = SubscriptionStatus.PENDING;

  @Column(name = "auto_renew", nullable = false)
  @Builder.Default
  private boolean autoRenew = false;
}
