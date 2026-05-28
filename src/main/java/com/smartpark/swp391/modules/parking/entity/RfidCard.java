package com.smartpark.swp391.modules.parking.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "rfid_cards")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class RfidCard extends TenantScopedEntity {

  @Column(nullable = false, length = 100)
  private String code;

  @Column(nullable = false, length = 100)
  private String uid;

  @Column(name = "qr_token", nullable = false, unique = true, length = 120)
  private String qrToken;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_user_id")
  private User assignedUser;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private RfidCardStatus status = RfidCardStatus.ACTIVE;

  @Column(name = "activated_at")
  private LocalDateTime activatedAt;

  @Column(name = "expired_at")
  private LocalDateTime expiredAt;
}
