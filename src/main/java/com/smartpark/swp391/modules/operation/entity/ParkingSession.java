package com.smartpark.swp391.modules.operation.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.vehicle.entity.UserVehicleLink;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "parking_sessions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class ParkingSession extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parking_id", nullable = false)
  private Parking parking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "zone_id", nullable = false)
  private Zone zone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "slot_id", nullable = false)
  private Slot slot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rfid_card_id")
  private RfidCard rfidCard;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_vehicle_link_id")
  private UserVehicleLink userVehicleLink;

  @Column(name = "license_plate", nullable = false, length = 30)
  private String licensePlate;

  @Column(name = "check_in_at", nullable = false)
  private LocalDateTime checkInAt;

  @Column(name = "check_out_at")
  private LocalDateTime checkOutAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private ParkingSessionStatus status = ParkingSessionStatus.ACTIVE;

  @Column(name = "entry_image_url", length = 1000)
  private String entryImageUrl;

  @Column(name = "license_plate_image_url", length = 1000)
  private String licensePlateImageUrl;

  @Column(name = "exit_image_url", length = 1000)
  private String exitImageUrl;

  @Column(name = "total_amount", precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", length = 20)
  private SessionPaymentStatus paymentStatus;

  @Column(name = "payment_method", length = 30)
  private String paymentMethod;

  @Column(name = "payment_reference", length = 100)
  private String paymentReference;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @Column(name = "exit_deadline")
  private LocalDateTime exitDeadline;
}
