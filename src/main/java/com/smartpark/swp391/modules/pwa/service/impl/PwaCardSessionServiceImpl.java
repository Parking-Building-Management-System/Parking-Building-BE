package com.smartpark.swp391.modules.pwa.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.payment.dto.ExistingPaymentIntentResponse;
import com.smartpark.swp391.modules.payment.service.PwaPaymentService;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.dto.CardCheckoutQuoteResponse;
import com.smartpark.swp391.modules.pwa.dto.report.OccupiedSlotReportRequest;
import com.smartpark.swp391.modules.pwa.dto.report.OccupiedSlotReportResponse;
import com.smartpark.swp391.modules.pwa.dto.report.PwaReportUploadRequest;
import com.smartpark.swp391.modules.pwa.dto.report.PwaReportUploadResponse;
import com.smartpark.swp391.modules.pwa.service.PwaCardSessionService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PwaCardSessionServiceImpl implements PwaCardSessionService {

  RfidCardRepository rfidCardRepository;
  ParkingSessionRepository parkingSessionRepository;
  SlotRepository slotRepository;
  StorageService storageService;
  PricingQuoteService pricingQuoteService;
  PwaPaymentService pwaPaymentService;
  PenaltyRuleLookupService penaltyRuleLookupService;
  PenaltyCaseRepository penaltyCaseRepository;

  @Override
  @Transactional(readOnly = true)
  public CardActiveSessionResponse getActiveSession(String qrToken) {
    RfidCard card =
        rfidCardRepository
            .findByQrToken(normalizeToken(qrToken))
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "CARD_QR_NOT_FOUND"));

    if (card.getStatus() != RfidCardStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "CARD_NOT_ACTIVE");
    }

    ParkingSession session =
        parkingSessionRepository
            .findActiveByRfidCardId(card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_CARD"));

    return toResponse(session);
  }

  @Override
  @Transactional(readOnly = true)
  public CardCheckoutQuoteResponse getCheckoutQuote(String qrToken) {
    RfidCard card = getActiveCard(qrToken);
    ParkingSession session = getActiveSessionForCard(card);
    LocalDateTime quotedAt = LocalDateTime.now();
    PricingQuoteResponse quote =
        pricingQuoteService.quote(
            session.getTenant().getId(),
            session.getParking().getId(),
            session.getVehicleType().getId(),
            session.getCheckInAt(),
            quotedAt);
    return toCheckoutQuoteResponse(session, quote);
  }

  @Override
  @Transactional(readOnly = true)
  public PwaReportUploadResponse createReportUpload(
      String qrToken, PwaReportUploadRequest request) {
    RfidCard card = getActiveCard(qrToken);
    ParkingSession session = getActiveSessionForCard(card);
    PresignedUpload upload =
        storageService.createPresignedUpload(
            session.getTenant().getId(),
            "parking-sessions/occupied-slot-reports",
            request.fileName(),
            request.contentType());
    return PwaReportUploadResponse.builder()
        .objectKey(upload.objectKey())
        .uploadUrl(upload.uploadUrl())
        .method(upload.method())
        .headers(upload.headers())
        .expiresInSeconds(upload.expiresInSeconds())
        .publicUrl(upload.publicUrl())
        .build();
  }

  @Override
  @Transactional
  public OccupiedSlotReportResponse reportOccupiedSlot(
      String qrToken, OccupiedSlotReportRequest request) {
    RfidCard card = getActiveCard(qrToken);
    ParkingSession victim = getActiveSessionForCard(card);
    if (victim.getStatus() != ParkingSessionStatus.ACTIVE) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "VICTIM_SESSION_NOT_ACTIVE");
    }

    String offenderPlate = normalizePlateForStorage(request.offenderPlateNumber());
    if (normalizePlateForCompare(offenderPlate)
        .equals(normalizePlateForCompare(victim.getLicensePlate()))) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "OFFENDER_PLATE_MATCHES_VICTIM");
    }

    Slot oldSlot = victim.getSlot();
    Parking parking = victim.getParking();
    PenaltyRule rule =
        penaltyRuleLookupService.requireActiveRule(
            victim.getTenant().getId(), parking.getId(), PenaltyType.OCCUPIED_ASSIGNED_SLOT);

    ParkingSession offender = findOffenderSession(victim, offenderPlate);
    Slot newSlot = findReplacementSlot(victim, oldSlot);

    victim.setSlot(newSlot);
    victim.setZone(newSlot.getZone());
    newSlot.setStatus(SlotStatus.OCCUPIED);
    oldSlot.setStatus(SlotStatus.OCCUPIED);

    PenaltyCase penaltyCase =
        PenaltyCase.builder()
            .tenant(victim.getTenant())
            .parking(parking)
            .rule(rule)
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(rule.getAmount())
            .currency(rule.getCurrency())
            .status(offender == null ? PenaltyCaseStatus.REPORTED : PenaltyCaseStatus.APPLIED)
            .targetSession(offender)
            .victimSession(victim)
            .offenderSession(offender)
            .reportedSlot(oldSlot)
            .reassignedSlot(newSlot)
            .targetLicensePlate(offender == null ? null : offender.getLicensePlate())
            .offenderLicensePlate(offenderPlate)
            .evidenceImageUrl(normalizeOptional(request.evidenceImageUrl()))
            .reportedFromPwa(true)
            .note(normalizeOptional(request.note()))
            .build();

    penaltyCase = penaltyCaseRepository.save(penaltyCase);
    slotRepository.save(oldSlot);
    slotRepository.save(newSlot);
    parkingSessionRepository.save(victim);

    boolean offenderMatched = offender != null;
    return OccupiedSlotReportResponse.builder()
        .message(
            offenderMatched
                ? "Thank you for your report. We recorded the violation and assigned you a new slot."
                : "Thank you for your report. We recorded the report and assigned you a new slot.")
        .oldSlotId(oldSlot.getId())
        .oldSlotCode(oldSlot.getCode())
        .newSlotId(newSlot.getId())
        .newSlotCode(newSlot.getCode())
        .offenderMatched(offenderMatched)
        .penaltyCaseId(penaltyCase.getId())
        .build();
  }

  private RfidCard getActiveCard(String qrToken) {
    RfidCard card =
        rfidCardRepository
            .findByQrToken(normalizeToken(qrToken))
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "CARD_QR_NOT_FOUND"));

    if (card.getStatus() != RfidCardStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "CARD_NOT_ACTIVE");
    }
    return card;
  }

  private ParkingSession getActiveSessionForCard(RfidCard card) {
    return parkingSessionRepository
        .findActiveByRfidCardId(card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_CARD"));
  }

  private ParkingSession findOffenderSession(ParkingSession victim, String offenderPlate) {
    String normalizedOffender = normalizePlateForCompare(offenderPlate);
    return parkingSessionRepository
        .findActiveDetailsByTenantIdAndParkingId(
            victim.getTenant().getId(), victim.getParking().getId(), ParkingSessionStatus.ACTIVE)
        .stream()
        .filter(candidate -> !candidate.getId().equals(victim.getId()))
        .filter(
            candidate ->
                normalizePlateForCompare(candidate.getLicensePlate()).equals(normalizedOffender))
        .findFirst()
        .orElse(null);
  }

  private Slot findReplacementSlot(ParkingSession victim, Slot oldSlot) {
    return slotRepository
        .findFirstAvailableForReassignmentByVehicleType(
            victim.getTenant().getId(),
            victim.getParking().getId(),
            victim.getVehicleType().getId(),
            oldSlot.getId(),
            SlotStatus.AVAILABLE,
            PageRequest.of(0, 1))
        .stream()
        .findFirst()
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_AVAILABLE_REPLACEMENT_SLOT"));
  }

  private CardActiveSessionResponse toResponse(ParkingSession session) {
    Parking parking = session.getParking();
    Zone zone = session.getZone();
    Slot slot = session.getSlot();
    Floor floor = slot.getFloor();
    String guideText = buildGuideText(floor, zone, slot);
    String mapImageUrl = mapImageUrl(floor);
    MapDisplay mapDisplay = resolveMapDisplay(session, mapImageUrl);

    return CardActiveSessionResponse.builder()
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .licensePlate(session.getLicensePlate())
        .cardCode(session.getRfidCard().getCode())
        .checkInAt(session.getCheckInAt())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .floorId(floor == null ? null : floor.getId())
        .floorName(floor == null ? null : floor.getName())
        .zoneId(zone.getId())
        .zoneName(zone.getName())
        .slotId(slot.getId())
        .slotCode(slot.getCode())
        .xCoordinate(slot.getXCoordinate())
        .yCoordinate(slot.getYCoordinate())
        .coordinateMode("PERCENT")
        .mapImageUrl(mapImageUrl)
        .mapDisplayUrl(mapDisplay.url())
        .mapUrlExpiresInSeconds(mapDisplay.expiresInSeconds())
        .status(session.getStatus())
        .guideText(guideText)
        .build();
  }

  private CardCheckoutQuoteResponse toCheckoutQuoteResponse(
      ParkingSession session, PricingQuoteResponse quote) {
    Parking parking = session.getParking();
    Zone zone = session.getZone();
    Slot slot = session.getSlot();
    Floor floor = slot.getFloor();
    VehicleType vehicleType = session.getVehicleType();

    return CardCheckoutQuoteResponse.builder()
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .licensePlate(session.getLicensePlate())
        .cardCode(session.getRfidCard().getCode())
        .status(session.getStatus())
        .checkInAt(session.getCheckInAt())
        .quotedAt(quote.quotedAt())
        .durationMinutes(quote.durationMinutes())
        .chargeableMinutes(quote.chargeableMinutes())
        .vehicleTypeId(vehicleType.getId())
        .vehicleTypeName(vehicleType.getName())
        .parkingName(parking.getName())
        .floorName(floor == null ? null : floor.getName())
        .zoneName(zone.getName())
        .slotCode(slot.getCode())
        .amount(quote.amount())
        .currency(quote.currency())
        .pricingRuleId(quote.pricingRuleId())
        .pricingRuleName(quote.pricingRuleName())
        .pricingBreakdown(quote.pricingBreakdown())
        .paymentAvailable(paymentAvailable(session, quote))
        .paymentStatus(session.getPaymentStatus())
        .paidAt(session.getPaidAt())
        .exitDeadline(session.getExitDeadline())
        .existingPaymentIntent(existingPaymentIntent(session, quote))
        .nextAction(nextPaymentAction(session, quote))
        .build();
  }

  private boolean paymentAvailable(ParkingSession session, PricingQuoteResponse quote) {
    if (session.getPaymentStatus() == SessionPaymentStatus.PAID) {
      return false;
    }
    return pwaPaymentService.paymentProviderAvailable()
        || existingPaymentIntent(session, quote) != null;
  }

  private ExistingPaymentIntentResponse existingPaymentIntent(
      ParkingSession session, PricingQuoteResponse quote) {
    return pwaPaymentService
        .findReusablePendingIntent(session.getId(), quote.amount())
        .orElse(null);
  }

  private String nextPaymentAction(ParkingSession session, PricingQuoteResponse quote) {
    if (session.getPaymentStatus() == SessionPaymentStatus.PAID) {
      return "EXIT_WITHIN_GRACE_PERIOD";
    }
    if (existingPaymentIntent(session, quote) != null) {
      return "CONTINUE_PAYMENT";
    }
    if (!pwaPaymentService.paymentProviderAvailable()) {
      return "PAYMENT_PROVIDER_DISABLED";
    }
    return "CREATE_PAYMENT_INTENT";
  }

  private String mapImageUrl(Floor floor) {
    if (floor == null || floor.getMapImageUrl() == null || floor.getMapImageUrl().isBlank()) {
      return null;
    }
    return floor.getMapImageUrl();
  }

  private MapDisplay resolveMapDisplay(ParkingSession session, String mapImageUrl) {
    if (mapImageUrl == null) {
      return new MapDisplay(null, null);
    }

    String normalizedMapImageUrl = mapImageUrl.trim();
    if (isHttpUrl(normalizedMapImageUrl)) {
      return new MapDisplay(normalizedMapImageUrl, null);
    }

    PresignedDownload download =
        storageService.createPresignedDownload(session.getTenant().getId(), normalizedMapImageUrl);
    return new MapDisplay(download.downloadUrl(), download.expiresInSeconds());
  }

  private boolean isHttpUrl(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.startsWith("http://") || lower.startsWith("https://");
  }

  private String buildGuideText(Floor floor, Zone zone, Slot slot) {
    if (floor == null) {
      return "Xe cua ban o khu " + zone.getName() + ", slot " + slot.getCode() + ".";
    }
    return "Xe cua ban o tang "
        + floor.getName()
        + ", khu "
        + zone.getName()
        + ", slot "
        + slot.getCode()
        + ".";
  }

  private String normalizeToken(String qrToken) {
    if (qrToken == null || qrToken.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "qrToken must not be blank");
    }
    return qrToken.trim();
  }

  private String normalizePlateForStorage(String plateNumber) {
    if (plateNumber == null || plateNumber.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "offenderPlateNumber must not be blank");
    }
    return plateNumber.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizePlateForCompare(String plateNumber) {
    if (plateNumber == null) {
      return "";
    }
    return plateNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record MapDisplay(String url, Long expiresInSeconds) {}
}
