package com.smartpark.swp391.modules.payment.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "payment_intents")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
@SQLRestriction("is_deleted = false")
public class PaymentIntent extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_session_id", nullable = false)
  private ParkingSession parkingSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rfid_card_id")
  private RfidCard rfidCard;

  @Column(name = "order_code", nullable = false, unique = true)
  private Long orderCode;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  @Builder.Default
  private String currency = "VND";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private PaymentIntentStatus status = PaymentIntentStatus.PENDING;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private PaymentProvider provider = PaymentProvider.PAYOS;

  @Column(name = "provider_payment_link_id")
  private String providerPaymentLinkId;

  @Column(name = "checkout_url", columnDefinition = "text")
  private String checkoutUrl;

  @Column(name = "qr_code", columnDefinition = "text")
  private String qrCode;

  @Column(length = 120)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pricing_rule_id")
  private PricingRule pricingRule;

  @Column(name = "quote_snapshot_json", columnDefinition = "text")
  private String quoteSnapshotJson;

  @Column(name = "raw_provider_response", columnDefinition = "text")
  private String rawProviderResponse;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean deleted = false;
}
