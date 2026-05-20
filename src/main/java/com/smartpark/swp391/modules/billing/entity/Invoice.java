package com.smartpark.swp391.modules.billing.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.billing.enumType.InvoiceStatus;
import com.smartpark.swp391.modules.billing.enumType.InvoiceType;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.subscription.entity.Subscription;
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
@Table(name = "invoice")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class Invoice extends TenantScopedEntity {

  @Column(name = "invoice_no", nullable = false, length = 50)
  private String invoiceNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id")
  private Subscription subscription;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_session_id")
  private ParkingSession parkingSession;

  @Enumerated(EnumType.STRING)
  @Column(name = "invoice_type", nullable = false, length = 30)
  private InvoiceType invoiceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private InvoiceStatus status = InvoiceStatus.DRAFT;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
  @Builder.Default
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "issued_at", nullable = false)
  private LocalDateTime issuedAt;

  @Column(name = "due_at")
  private LocalDateTime dueAt;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;
}
