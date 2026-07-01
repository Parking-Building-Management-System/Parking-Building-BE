package com.smartpark.swp391.modules.penalty.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
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
@Table(name = "parking_penalty_cases")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class PenaltyCase extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rule_id")
  private PenaltyRule rule;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private PenaltyType type;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 10)
  @Builder.Default
  private String currency = "VND";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PenaltyCaseStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_session_id")
  private ParkingSession targetSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "victim_session_id")
  private ParkingSession victimSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offender_session_id")
  private ParkingSession offenderSession;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_slot_id")
  private Slot reportedSlot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reassigned_slot_id")
  private Slot reassignedSlot;

  @Column(name = "target_license_plate", length = 30)
  private String targetLicensePlate;

  @Column(name = "offender_license_plate", length = 30)
  private String offenderLicensePlate;

  @Column(name = "evidence_image_url", length = 1000)
  private String evidenceImageUrl;

  @Column(name = "identity_image_url", length = 1000)
  private String identityImageUrl;

  @Column(name = "vehicle_image_url", length = 1000)
  private String vehicleImageUrl;

  @Column(name = "license_plate_image_url", length = 1000)
  private String licensePlateImageUrl;

  @Column(name = "reported_from_pwa", nullable = false)
  @Builder.Default
  private boolean reportedFromPwa = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_by_staff_id")
  private User reportedByStaff;

  @Column(length = 1000)
  private String note;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "collected_at")
  private LocalDateTime collectedAt;
}
