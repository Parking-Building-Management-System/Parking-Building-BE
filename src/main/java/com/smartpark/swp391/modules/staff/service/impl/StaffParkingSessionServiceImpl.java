package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
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
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.exit.ExitDecision;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPaymentMode;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewRequest;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewResponse;
import com.smartpark.swp391.modules.staff.service.StaffParkingSessionService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
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
  PricingQuoteService pricingQuoteService;
  PricingRuleRepository pricingRuleRepository;

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

    SlotAssignment slotAssignment =
        assignSlot(tenantId, parking.getId(), request.vehicleTypeId());
    Slot slot = slotAssignment.slot();
    VehicleType vehicleType = slotAssignment.vehicleType();
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

  @Override
  @Transactional(readOnly = true)
  public ExitPreviewResponse previewExit(ExitPreviewRequest request) {
    UUID tenantId = currentTenantId();
    StaffWorkContextResponse workContext = requireExitWorkContext();
    RfidCard card = getTenantCard(tenantId, request.cardCode());
    ParkingSession session = getActiveSessionForExit(tenantId, card);
    validateSessionInKioskParking(session, workContext.parkingId());
    ExitPreview preview = buildExitPreview(session, card, LocalDateTime.now());
    return preview.response();
  }

  @Override
  @Transactional
  public CompleteExitResponse completeExit(CompleteExitRequest request) {
    UUID tenantId = currentTenantId();
    StaffWorkContextResponse workContext = requireExitWorkContext();
    ParkingSession session =
        parkingSessionRepository
            .findDetailByTenantIdAndId(tenantId, request.sessionId())
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PARKING_SESSION_NOT_FOUND"));

    if (session.getStatus() == ParkingSessionStatus.COMPLETED) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "SESSION_ALREADY_COMPLETED");
    }
    if (session.getStatus() != ParkingSessionStatus.ACTIVE) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "SESSION_NOT_ACTIVE");
    }
    validateSessionInKioskParking(session, workContext.parkingId());

    RfidCard card = session.getRfidCard();
    if (card == null || !card.getCode().equalsIgnoreCase(normalizeCardCode(request.cardCode()))) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "CARD_CODE_DOES_NOT_MATCH_SESSION");
    }

    LocalDateTime now = LocalDateTime.now();
    ExitPreview preview = buildExitPreview(session, card, now);
    validatePaymentMode(request, preview.response());

    BigDecimal totalAmount = finalTotalAmount(session, request, preview.response());
    session.setCheckOutAt(now);
    session.setStatus(ParkingSessionStatus.COMPLETED);
    session.setTotalAmount(totalAmount);
    applyCashPaymentState(session, request.paymentMode());

    Slot slot = session.getSlot();
    slot.setStatus(SlotStatus.AVAILABLE);
    slotRepository.save(slot);
    parkingSessionRepository.save(session);

    return CompleteExitResponse.builder()
        .sessionId(session.getId())
        .status(session.getStatus())
        .plateNumber(session.getLicensePlate())
        .cardCode(card.getCode())
        .checkInAt(session.getCheckInAt())
        .checkOutAt(session.getCheckOutAt())
        .paymentMode(request.paymentMode())
        .collectedAmount(request.collectedAmount())
        .totalAmount(totalAmount)
        .currency(preview.response().currency())
        .slotCode(slot.getCode())
        .slotStatus(slot.getStatus())
        .cardStatus(card.getStatus())
        .message("Exit completed. Gate can open.")
        .build();
  }

  private SlotAssignment assignSlot(UUID tenantId, UUID parkingId, UUID vehicleTypeId) {
    if (vehicleTypeId != null) {
      VehicleType vehicleType = getActiveVehicleType(vehicleTypeId);
      Slot slot =
          slotRepository
              .findFirstAvailableForCheckInByVehicleType(
                  tenantId,
                  parkingId,
                  vehicleType.getId(),
                  SlotStatus.AVAILABLE,
                  PageRequest.of(0, 1))
              .stream()
              .findFirst()
              .orElseThrow(
                  () ->
                      new ApiException(
                          ErrorCode.RESOURCE_NOT_FOUND,
                          "No available slot for selected vehicle type"));
      return new SlotAssignment(slot, vehicleType);
    }

    Slot slot =
        slotRepository
            .findFirstAvailableForCheckIn(
                tenantId, parkingId, SlotStatus.AVAILABLE, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No available slot"));
    return new SlotAssignment(slot, resolveVehicleTypeFromSlot(slot));
  }

  private VehicleType getActiveVehicleType(UUID vehicleTypeId) {
    VehicleType vehicleType =
        vehicleTypeRepository
            .findByIdAndDeletedFalse(vehicleTypeId)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vehicle type not found"));
    if (!vehicleType.isActive()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Vehicle type is inactive");
    }
    return vehicleType;
  }

  private VehicleType resolveVehicleTypeFromSlot(Slot slot) {
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
        .vehicleTypeId(session.getVehicleType().getId())
        .vehicleTypeCode(session.getVehicleType().getCode())
        .vehicleTypeName(session.getVehicleType().getName())
        .parkingId(slot.getParking().getId())
        .entryTime(session.getCheckInAt())
        .status(session.getStatus())
        .build();
  }

  private StaffWorkContextResponse requireExitWorkContext() {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    if (workContext.kioskType() != KioskType.EXIT && workContext.kioskType() != KioskType.MIXED) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "EXIT_KIOSK_REQUIRED");
    }
    return workContext;
  }

  private RfidCard getTenantCard(UUID tenantId, String cardCode) {
    return rfidCardRepository
        .findByTenantIdAndCodeIgnoreCase(tenantId, normalizeCardCode(cardCode))
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "RFID_CARD_NOT_FOUND"));
  }

  private ParkingSession getActiveSessionForExit(UUID tenantId, RfidCard card) {
    return parkingSessionRepository
        .findActiveDetailByTenantAndRfidCardId(
            tenantId, card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_CARD"));
  }

  private void validateSessionInKioskParking(ParkingSession session, UUID kioskParkingId) {
    if (!session.getParking().getId().equals(kioskParkingId)) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "SESSION_NOT_IN_KIOSK_PARKING");
    }
  }

  private ExitPreview buildExitPreview(
      ParkingSession session, RfidCard card, LocalDateTime evaluatedAt) {
    PricingQuoteResponse quote =
        pricingQuoteService.quote(
            session.getTenant().getId(),
            session.getParking().getId(),
            session.getVehicleType().getId(),
            session.getCheckInAt(),
            evaluatedAt);
    PricingRule pricingRule =
        pricingRuleRepository
            .findById(quote.pricingRuleId())
            .orElseThrow(
                () ->
                    new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PRICING_RULE_NOT_CONFIGURED"));

    BigDecimal surcharge = BigDecimal.ZERO;
    ExitDecision decision;
    BigDecimal amountDue = BigDecimal.ZERO;
    String message;

    if (session.getPaymentStatus() == SessionPaymentStatus.PAID) {
      if (session.getExitDeadline() != null && !evaluatedAt.isAfter(session.getExitDeadline())) {
        decision = ExitDecision.ALLOW_EXIT;
        message = "Paid online. Allow exit.";
      } else {
        decision = ExitDecision.GRACE_EXPIRED_SURCHARGE;
        surcharge = pricingRule.getNextBlockPrice();
        message = "Grace period expired. Collect surcharge.";
      }
    } else {
      decision = ExitDecision.COLLECT_CASH;
      amountDue = quote.amount();
      message = "Cash payment required.";
    }

    ExitPreviewResponse response =
        ExitPreviewResponse.builder()
            .sessionId(session.getId())
            .plateNumber(session.getLicensePlate())
            .cardCode(card.getCode())
            .parkingName(session.getParking().getName())
            .floorName(
                session.getSlot().getFloor() == null
                    ? null
                    : session.getSlot().getFloor().getName())
            .zoneName(session.getZone().getName())
            .slotCode(session.getSlot().getCode())
            .checkInAt(session.getCheckInAt())
            .paidAt(session.getPaidAt())
            .exitDeadline(session.getExitDeadline())
            .paymentStatus(session.getPaymentStatus())
            .exitDecision(decision)
            .amountDue(amountDue)
            .surchargeAmount(surcharge)
            .totalAmount(resolveCurrentPaidAmount(session, quote, surcharge, decision))
            .currency(quote.currency())
            .message(message)
            .build();
    return new ExitPreview(response, quote);
  }

  private BigDecimal resolveCurrentPaidAmount(
      ParkingSession session,
      PricingQuoteResponse quote,
      BigDecimal surcharge,
      ExitDecision decision) {
    if (decision == ExitDecision.COLLECT_CASH) {
      return quote.amount();
    }
    BigDecimal paid = session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
    if (decision == ExitDecision.GRACE_EXPIRED_SURCHARGE) {
      return paid.add(surcharge);
    }
    return paid;
  }

  private void validatePaymentMode(CompleteExitRequest request, ExitPreviewResponse preview) {
    switch (request.paymentMode()) {
      case ONLINE -> {
        if (preview.exitDecision() != ExitDecision.ALLOW_EXIT) {
          throw new ApiException(ErrorCode.INVALID_INPUT, "ONLINE_EXIT_NOT_ALLOWED");
        }
        if (request.collectedAmount().compareTo(BigDecimal.ZERO) != 0) {
          throw new ApiException(ErrorCode.INVALID_INPUT, "ONLINE_COLLECTED_AMOUNT_MUST_BE_ZERO");
        }
      }
      case CASH -> {
        if (preview.exitDecision() != ExitDecision.COLLECT_CASH) {
          throw new ApiException(ErrorCode.INVALID_INPUT, "CASH_EXIT_NOT_ALLOWED");
        }
        requireCollectedAtLeast(
            request.collectedAmount(), preview.amountDue(), "CASH_AMOUNT_TOO_LOW");
      }
      case SURCHARGE_CASH -> {
        if (preview.exitDecision() != ExitDecision.GRACE_EXPIRED_SURCHARGE) {
          throw new ApiException(ErrorCode.INVALID_INPUT, "SURCHARGE_EXIT_NOT_ALLOWED");
        }
        requireCollectedAtLeast(
            request.collectedAmount(), preview.surchargeAmount(), "SURCHARGE_AMOUNT_TOO_LOW");
      }
    }
  }

  private void requireCollectedAtLeast(
      BigDecimal collectedAmount, BigDecimal requiredAmount, String message) {
    if (collectedAmount.compareTo(requiredAmount) < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, message);
    }
  }

  private BigDecimal finalTotalAmount(
      ParkingSession session, CompleteExitRequest request, ExitPreviewResponse preview) {
    return switch (request.paymentMode()) {
      case ONLINE -> session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
      case CASH -> preview.amountDue();
      case SURCHARGE_CASH -> {
        BigDecimal onlinePaid =
            session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
        yield onlinePaid.add(preview.surchargeAmount());
      }
    };
  }

  private void applyCashPaymentState(ParkingSession session, ExitPaymentMode paymentMode) {
    if (paymentMode == ExitPaymentMode.CASH) {
      session.setPaymentStatus(SessionPaymentStatus.CASH_COLLECTED);
      session.setPaymentMethod("CASH");
      session.setPaidAt(LocalDateTime.now());
    } else if (paymentMode == ExitPaymentMode.SURCHARGE_CASH) {
      session.setPaymentStatus(SessionPaymentStatus.SURCHARGE_COLLECTED);
      session.setPaymentMethod("PAYOS+CASH");
    }
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private UUID resolveParkingId(ParkingSessionCheckInRequest request) {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    if (request.parkingId() == null) {
      return workContext.parkingId();
    }
    if (!request.parkingId().equals(workContext.parkingId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "CHECK_IN_PARKING_NOT_IN_KIOSK_CONTEXT");
    }
    return request.parkingId();
  }

  private String normalizePlate(String plateNumber) {
    return plateNumber.trim().toUpperCase();
  }

  private String normalizeCardCode(String cardCode) {
    if (cardCode == null || cardCode.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "cardCode must not be blank");
    }
    return cardCode.trim();
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record SlotAssignment(Slot slot, VehicleType vehicleType) {}

  private record ExitPreview(ExitPreviewResponse response, PricingQuoteResponse quote) {}
}
