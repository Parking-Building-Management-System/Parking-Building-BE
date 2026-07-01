package com.smartpark.swp391.modules.settlement.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "staff_cash_shifts")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class StaffCashShift extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "kiosk_id", nullable = false)
  private Kiosk kiosk;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "staff_id", nullable = false)
  private User staff;

  @Column(name = "opened_at", nullable = false)
  private LocalDateTime openedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private StaffCashShiftStatus status = StaffCashShiftStatus.OPEN;

  @Column(name = "expected_cash_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal expectedCashAmount = BigDecimal.ZERO;

  @Column(name = "counted_cash_amount", precision = 12, scale = 2)
  private BigDecimal countedCashAmount;

  @Column(name = "variance_amount", precision = 12, scale = 2)
  private BigDecimal varianceAmount;

  @Column(name = "online_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal onlineAmount = BigDecimal.ZERO;

  @Column(name = "cash_parking_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal cashParkingAmount = BigDecimal.ZERO;

  @Column(name = "surcharge_cash_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal surchargeCashAmount = BigDecimal.ZERO;

  @Column(name = "penalty_cash_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal penaltyCashAmount = BigDecimal.ZERO;

  @Column(name = "lost_card_cash_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal lostCardCashAmount = BigDecimal.ZERO;

  @Column(name = "transaction_count", nullable = false)
  @Builder.Default
  private Integer transactionCount = 0;

  @Column(length = 1000)
  private String note;
}
