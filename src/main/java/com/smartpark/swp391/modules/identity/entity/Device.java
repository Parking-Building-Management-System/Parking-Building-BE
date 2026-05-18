package com.smartpark.swp391.modules.identity.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "devices",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "fingerprint"})})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Device extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 255)
  private String fingerprint;

  @Column(length = 100)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private DeviceStatus status = DeviceStatus.PENDING;

  @Column(name = "approved_by")
  private UUID approvedBy;

  // Đổi sang LocalDateTime cho đồng bộ với cái BaseEntity của ông
  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
}
