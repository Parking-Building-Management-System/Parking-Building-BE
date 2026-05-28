package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.service.StaffParkingSessionService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffParkingSessionServiceImpl implements StaffParkingSessionService {

  ParkingSessionRepository parkingSessionRepository;
  ParkingRepository parkingRepository;
  RfidCardRepository rfidCardRepository;
  SlotRepository slotRepository;
  VehicleTypeRepository vehicleTypeRepository;
  TenantRepository tenantRepository;
  StaffWorkContextService staffWorkContextService;

  @Override
  @Transactional
  public ParkingSessionCheckInResponse checkIn(ParkingSessionCheckInRequest request) {
    UUID tenantId = currentTenantId();
    UUID parkingId = resolveParkingId(request);
    Tenant tenant = tenantRepository.getReferenceById(tenantId);
    Parking parking =
        parkingRepository
            .findByIdAndTenantIdAndIsDeletedFalse(parkingId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));

    RfidCard card =
        rfidCardRepository
            .findByTenantIdAndCodeIgnoreCase(tenantId, request.cardCode().trim())
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "RFID card not found"));

    if (card.getStatus() != RfidCardStatus.ACTIVE) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "RFID card is not available");
    }
    if (parkingSessionRepository.existsByTenantIdAndRfidCardIdAndStatus(
        tenantId, card.getId(), ParkingSessionStatus.ACTIVE)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "RFID card is already in use");
    }

    Slot slot =
        slotRepository
            .findFirstAvailableForCheckIn(
                tenantId, parking.getId(), SlotStatus.AVAILABLE, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No available slot"));

    VehicleType vehicleType = resolveVehicleType(request.vehicleTypeId(), slot);
    LocalDateTime now = LocalDateTime.now();

    ParkingSession session =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .zone(slot.getZone())
            .slot(slot)
            .rfidCard(card)
            .vehicleType(vehicleType)
            .licensePlate(normalizePlate(request.plateNumber()))
            .checkInAt(now)
            .status(ParkingSessionStatus.ACTIVE)
            .entryImageUrl(normalizeOptional(request.entryImageUrl()))
            .build();

    ParkingSession saved = parkingSessionRepository.save(session);
    slot.setStatus(SlotStatus.OCCUPIED);
    slotRepository.save(slot);

    return toResponse(saved, card, slot);
  }

  private VehicleType resolveVehicleType(UUID vehicleTypeId, Slot slot) {
    if (vehicleTypeId != null) {
      VehicleType vehicleType =
          vehicleTypeRepository
              .findById(vehicleTypeId)
              .orElseThrow(
                  () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vehicle type not found"));
      if (!vehicleType.isActive()) {
        throw new ApiException(ErrorCode.INVALID_INPUT, "Vehicle type is inactive");
      }
      return vehicleType;
    }

    VehicleType zoneVehicleType = slot.getZone().getVehicleType();
    if (zoneVehicleType == null) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "vehicleTypeId is required for the selected zone");
    }
    if (!zoneVehicleType.isActive()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Vehicle type is inactive");
    }
    return zoneVehicleType;
  }

  private ParkingSessionCheckInResponse toResponse(
      ParkingSession session, RfidCard card, Slot slot) {
    Zone zone = slot.getZone();
    return ParkingSessionCheckInResponse.builder()
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .cardCode(card.getCode())
        .qrToken(card.getQrToken())
        .pwaAccessPath("/pwa/c/" + card.getQrToken())
        .assignedSlotId(slot.getId())
        .assignedSlotCode(slot.getCode())
        .zoneId(zone.getId())
        .zoneName(zone.getName())
        .parkingId(slot.getParking().getId())
        .entryTime(session.getCheckInAt())
        .status(session.getStatus())
        .build();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private UUID resolveParkingId(ParkingSessionCheckInRequest request) {
    if (request.parkingId() != null) {
      return request.parkingId();
    }
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    return workContext.parkingId();
  }

  private String normalizePlate(String plateNumber) {
    return plateNumber.trim().toUpperCase();
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
