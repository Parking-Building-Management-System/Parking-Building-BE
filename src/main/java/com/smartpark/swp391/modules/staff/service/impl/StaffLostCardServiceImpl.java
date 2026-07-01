package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyCaseResponseMapper;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionSource;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import com.smartpark.swp391.modules.settlement.service.StaffCashLedgerEntry;
import com.smartpark.swp391.modules.settlement.service.StaffCashLedgerService;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPreviewResponse;
import com.smartpark.swp391.modules.staff.service.StaffLostCardService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffLostCardServiceImpl implements StaffLostCardService {

  StaffWorkContextService staffWorkContextService;
  ParkingSessionRepository parkingSessionRepository;
  PricingQuoteService pricingQuoteService;
  PricingRuleRepository pricingRuleRepository;
  PenaltyRuleLookupService penaltyRuleLookupService;
  PenaltyCaseRepository penaltyCaseRepository;
  PenaltyCaseResponseMapper penaltyCaseResponseMapper;
  SlotRepository slotRepository;
  RfidCardRepository rfidCardRepository;
  StorageService storageService;
  StaffCashLedgerService staffCashLedgerService;

  @Override
  @Transactional(readOnly = true)
  public StaffLostCardPhotoPresignResponse createPhotoUpload(
      StaffLostCardPhotoPresignRequest request) {
    requireExitWorkContext();
    PresignedUpload upload =
        storageService.createPresignedUpload(
            currentTenantId(),
            "parking-sessions/lost-card/" + normalizePhotoFolder(request.photoType()),
            request.fileName(),
            request.contentType());
    return StaffLostCardPhotoPresignResponse.builder()
        .objectKey(upload.objectKey())
        .uploadUrl(upload.uploadUrl())
        .method(upload.method())
        .headers(upload.headers())
        .expiresInSeconds(upload.expiresInSeconds())
        .publicUrl(upload.publicUrl())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public StaffLostCardPreviewResponse previewByPlate(String plateNumber) {
    StaffWorkContextResponse workContext = requireExitWorkContext();
    ParkingSession session = findActiveSessionByPlate(workContext.parkingId(), plateNumber);
    PenaltyRule lostCardRule =
        penaltyRuleLookupService.requireActiveRule(
            currentTenantId(), workContext.parkingId(), PenaltyType.LOST_CARD);
    DueAmounts dueAmounts = calculateDue(session);
    List<PenaltyCase> existingPenalties = unpaidPenaltyCases(session);
    BigDecimal existingPenaltyAmount = sumPenaltyAmount(existingPenalties);
    BigDecimal totalDueIfLostCard =
        dueAmounts
            .cashParkingDue()
            .add(dueAmounts.surchargeDue())
            .add(existingPenaltyAmount)
            .add(lostCardRule.getAmount());

    return StaffLostCardPreviewResponse.builder()
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .vehicleTypeId(session.getVehicleType().getId())
        .vehicleType(session.getVehicleType().getName())
        .parkingId(session.getParking().getId())
        .parkingName(session.getParking().getName())
        .zoneId(session.getZone().getId())
        .zoneName(session.getZone().getName())
        .slotId(session.getSlot().getId())
        .slotCode(session.getSlot().getCode())
        .checkInAt(session.getCheckInAt())
        .entryImageUrl(resolveImageDisplayUrl(session.getEntryImageUrl()))
        .licensePlateImageUrl(resolveImageDisplayUrl(session.getLicensePlateImageUrl()))
        .parkingAmountDue(dueAmounts.cashParkingDue())
        .surchargeAmountDue(dueAmounts.surchargeDue())
        .existingPenaltyAmount(existingPenaltyAmount)
        .lostCardPenaltyAmount(lostCardRule.getAmount())
        .totalDueIfLostCard(totalDueIfLostCard)
        .currency(lostCardRule.getCurrency())
        .currentRfidCardCode(maskCardCode(session.getRfidCard()))
        .activePenaltyCases(
            existingPenalties.stream().map(penaltyCaseResponseMapper::toResponse).toList())
        .build();
  }

  @Override
  @Transactional
  public StaffLostCardCaseResponse createCase(StaffLostCardCaseRequest request) {
    StaffWorkContextResponse workContext = requireExitWorkContext();
    ParkingSession session = getActiveSession(request.sessionId(), workContext.parkingId());
    PenaltyRule rule =
        penaltyRuleLookupService.requireActiveRule(
            currentTenantId(), workContext.parkingId(), PenaltyType.LOST_CARD);
    assertNoActiveLostCardCase(session);

    PenaltyCase penaltyCase =
        PenaltyCase.builder()
            .tenant(session.getTenant())
            .parking(session.getParking())
            .rule(rule)
            .type(PenaltyType.LOST_CARD)
            .amount(rule.getAmount())
            .currency(rule.getCurrency())
            .status(PenaltyCaseStatus.APPLIED)
            .targetSession(session)
            .targetLicensePlate(session.getLicensePlate())
            .identityImageUrl(normalizeOptional(request.identityImageUrl()))
            .vehicleImageUrl(normalizeOptional(request.vehicleImageUrl()))
            .licensePlateImageUrl(normalizeOptional(request.licensePlateImageUrl()))
            .note(normalizeOptional(request.note()))
            .build();
    penaltyCase = penaltyCaseRepository.save(penaltyCase);

    return StaffLostCardCaseResponse.builder()
        .penaltyCase(penaltyCaseResponseMapper.toResponse(penaltyCase))
        .message("Lost card penalty case created.")
        .build();
  }

  @Override
  @Transactional
  public StaffLostCardCompleteExitResponse completeExit(StaffLostCardCompleteExitRequest request) {
    StaffResolvedContext workContext = requireExitResolvedWorkContext();
    ParkingSession session = getActiveSession(request.sessionId(), workContext.parkingId());
    PenaltyCase lostCardCase =
        penaltyCaseRepository
            .findDetailByTenantIdAndId(currentTenantId(), request.lostCardCaseId())
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "LOST_CARD_CASE_NOT_FOUND"));
    validateLostCardCase(lostCardCase, session);

    LocalDateTime now = LocalDateTime.now();
    DueAmounts dueAmounts = calculateDue(session);
    List<PenaltyCase> penalties = unpaidPenaltyCases(session);
    BigDecimal penaltyAmountDue = sumPenaltyAmount(penalties);
    BigDecimal totalAmountDue =
        dueAmounts.cashParkingDue().add(dueAmounts.surchargeDue()).add(penaltyAmountDue);
    requireCollectedAtLeast(request.collectedAmount(), totalAmountDue);

    List<StaffCashLedgerEntry> ledgerEntries =
        ledgerEntriesForLostCardExit(session, dueAmounts, penalties, request.note());
    if (!ledgerEntries.isEmpty()) {
      staffCashLedgerService.recordCashTransactions(workContext, ledgerEntries);
    }
    markPenaltyCasesCollected(penalties, now);
    session.setCheckOutAt(now);
    session.setStatus(ParkingSessionStatus.COMPLETED);
    session.setTotalAmount(dueAmounts.finalParkingTotal());
    applyParkingPaymentState(session, dueAmounts);

    Slot slot = session.getSlot();
    slot.setStatus(SlotStatus.AVAILABLE);
    slotRepository.save(slot);

    RfidCard card = session.getRfidCard();
    if (card != null) {
      card.setStatus(RfidCardStatus.LOST);
      rfidCardRepository.save(card);
    }
    parkingSessionRepository.save(session);

    return StaffLostCardCompleteExitResponse.builder()
        .sessionId(session.getId())
        .status(session.getStatus())
        .plateNumber(session.getLicensePlate())
        .checkOutAt(session.getCheckOutAt())
        .parkingAmountDue(dueAmounts.cashParkingDue())
        .surchargeAmountDue(dueAmounts.surchargeDue())
        .penaltyAmountDue(penaltyAmountDue)
        .totalAmountDue(totalAmountDue)
        .collectedAmount(request.collectedAmount())
        .currency(dueAmounts.currency())
        .slotCode(slot.getCode())
        .slotStatus(slot.getStatus())
        .cardCode(card == null ? null : card.getCode())
        .cardStatus(card == null ? null : card.getStatus())
        .message("Lost card exit completed. Gate can open.")
        .build();
  }

  private StaffWorkContextResponse requireExitWorkContext() {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    if (workContext.kioskType() != KioskType.EXIT && workContext.kioskType() != KioskType.MIXED) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "EXIT_KIOSK_REQUIRED");
    }
    return workContext;
  }

  private StaffResolvedContext requireExitResolvedWorkContext() {
    StaffResolvedContext workContext = staffWorkContextService.requireCurrentResolvedContext();
    if (workContext.kioskType() != KioskType.EXIT && workContext.kioskType() != KioskType.MIXED) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "EXIT_KIOSK_REQUIRED");
    }
    return workContext;
  }

  private ParkingSession findActiveSessionByPlate(UUID parkingId, String plateNumber) {
    String normalizedPlate = normalizePlateForCompare(plateNumber);
    return parkingSessionRepository
        .findActiveDetailsByTenantIdAndParkingId(
            currentTenantId(), parkingId, ParkingSessionStatus.ACTIVE)
        .stream()
        .filter(
            session -> normalizePlateForCompare(session.getLicensePlate()).equals(normalizedPlate))
        .findFirst()
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_PLATE"));
  }

  private ParkingSession getActiveSession(UUID sessionId, UUID parkingId) {
    ParkingSession session =
        parkingSessionRepository
            .findDetailByTenantIdAndId(currentTenantId(), sessionId)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PARKING_SESSION_NOT_FOUND"));
    if (session.getStatus() != ParkingSessionStatus.ACTIVE) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "SESSION_NOT_ACTIVE");
    }
    if (!session.getParking().getId().equals(parkingId)) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "SESSION_NOT_IN_KIOSK_PARKING");
    }
    return session;
  }

  private DueAmounts calculateDue(ParkingSession session) {
    LocalDateTime now = LocalDateTime.now();
    PricingQuoteResponse quote =
        pricingQuoteService.quote(
            session.getTenant().getId(),
            session.getParking().getId(),
            session.getVehicleType().getId(),
            session.getCheckInAt(),
            now);
    PricingRule pricingRule =
        pricingRuleRepository
            .findById(quote.pricingRuleId())
            .orElseThrow(
                () ->
                    new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PRICING_RULE_NOT_CONFIGURED"));

    BigDecimal paid = session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
    if (session.getPaymentStatus() == SessionPaymentStatus.PAID) {
      if (session.getExitDeadline() != null && !now.isAfter(session.getExitDeadline())) {
        return new DueAmounts(BigDecimal.ZERO, BigDecimal.ZERO, paid, quote.currency());
      }
      BigDecimal surcharge = pricingRule.getNextBlockPrice();
      return new DueAmounts(BigDecimal.ZERO, surcharge, paid.add(surcharge), quote.currency());
    }
    return new DueAmounts(quote.amount(), BigDecimal.ZERO, quote.amount(), quote.currency());
  }

  private List<PenaltyCase> unpaidPenaltyCases(ParkingSession session) {
    return penaltyCaseRepository.findByTargetSessionAndStatuses(
        currentTenantId(), session.getId(), List.of(PenaltyCaseStatus.APPLIED));
  }

  private BigDecimal sumPenaltyAmount(List<PenaltyCase> penaltyCases) {
    return penaltyCases.stream()
        .map(PenaltyCase::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void assertNoActiveLostCardCase(ParkingSession session) {
    boolean duplicate =
        unpaidPenaltyCases(session).stream()
            .anyMatch(penaltyCase -> penaltyCase.getType() == PenaltyType.LOST_CARD);
    if (duplicate) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "LOST_CARD_CASE_ALREADY_APPLIED");
    }
  }

  private void validateLostCardCase(PenaltyCase lostCardCase, ParkingSession session) {
    if (lostCardCase.getType() != PenaltyType.LOST_CARD) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "PENALTY_CASE_IS_NOT_LOST_CARD");
    }
    if (lostCardCase.getStatus() != PenaltyCaseStatus.APPLIED) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "LOST_CARD_CASE_NOT_APPLIED");
    }
    if (lostCardCase.getTargetSession() == null
        || !lostCardCase.getTargetSession().getId().equals(session.getId())) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "LOST_CARD_CASE_SESSION_MISMATCH");
    }
  }

  private void markPenaltyCasesCollected(
      List<PenaltyCase> penaltyCases, LocalDateTime collectedAt) {
    penaltyCases.forEach(
        penaltyCase -> {
          penaltyCase.setStatus(PenaltyCaseStatus.COLLECTED);
          penaltyCase.setCollectedAt(collectedAt);
          penaltyCase.setResolvedAt(collectedAt);
        });
    penaltyCaseRepository.saveAll(penaltyCases);
  }

  private List<StaffCashLedgerEntry> ledgerEntriesForLostCardExit(
      ParkingSession session, DueAmounts dueAmounts, List<PenaltyCase> penalties, String note) {
    List<StaffCashLedgerEntry> entries = new ArrayList<>();
    addEntry(
        entries,
        StaffCashTransactionType.PARKING_CASH,
        dueAmounts.cashParkingDue(),
        session,
        null,
        StaffCashTransactionSource.LOST_CARD_EXIT,
        note);
    addEntry(
        entries,
        StaffCashTransactionType.SURCHARGE_CASH,
        dueAmounts.surchargeDue(),
        session,
        null,
        StaffCashTransactionSource.LOST_CARD_EXIT,
        note);
    penalties.forEach(
        penaltyCase ->
            addEntry(
                entries,
                penaltyCase.getType() == PenaltyType.LOST_CARD
                    ? StaffCashTransactionType.LOST_CARD_FINE
                    : StaffCashTransactionType.PENALTY_CASH,
                penaltyCase.getAmount(),
                session,
                penaltyCase,
                StaffCashTransactionSource.LOST_CARD_EXIT,
                note));
    return entries;
  }

  private void addEntry(
      List<StaffCashLedgerEntry> entries,
      StaffCashTransactionType type,
      BigDecimal amount,
      ParkingSession session,
      PenaltyCase penaltyCase,
      StaffCashTransactionSource source,
      String note) {
    if (amount == null || amount.signum() <= 0) {
      return;
    }
    entries.add(new StaffCashLedgerEntry(type, amount, session, penaltyCase, source, note));
  }

  private void applyParkingPaymentState(ParkingSession session, DueAmounts dueAmounts) {
    if (dueAmounts.cashParkingDue().signum() > 0) {
      session.setPaymentStatus(SessionPaymentStatus.CASH_COLLECTED);
      session.setPaymentMethod("CASH");
      session.setPaidAt(LocalDateTime.now());
    } else if (dueAmounts.surchargeDue().signum() > 0) {
      session.setPaymentStatus(SessionPaymentStatus.SURCHARGE_COLLECTED);
      session.setPaymentMethod("PAYOS+CASH");
    }
  }

  private void requireCollectedAtLeast(BigDecimal collectedAmount, BigDecimal requiredAmount) {
    if (collectedAmount.compareTo(requiredAmount) < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "COLLECTED_AMOUNT_TOO_LOW");
    }
  }

  private String resolveImageDisplayUrl(String imageRef) {
    String normalized = normalizeOptional(imageRef);
    if (normalized == null) {
      return null;
    }
    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return normalized;
    }
    if (normalized.startsWith("tenants/" + currentTenantId() + "/")) {
      try {
        PresignedDownload download =
            storageService.createPresignedDownload(currentTenantId(), normalized);
        return download.downloadUrl();
      } catch (ApiException e) {
        return null;
      }
    }
    return normalized;
  }

  private String maskCardCode(RfidCard card) {
    if (card == null || card.getCode() == null || card.getCode().length() <= 4) {
      return card == null ? null : card.getCode();
    }
    String code = card.getCode();
    return "***" + code.substring(code.length() - 4);
  }

  private String normalizePhotoFolder(String photoType) {
    String normalized = photoType.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "IDENTITY", "IDENTITY_DOCUMENT", "ID" -> "identity-document";
      case "VEHICLE" -> "vehicle";
      case "LICENSE_PLATE", "PLATE" -> "license-plate";
      default -> "other";
    };
  }

  private String normalizePlateForCompare(String plateNumber) {
    if (plateNumber == null || plateNumber.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "plateNumber must not be blank");
    }
    return plateNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private record DueAmounts(
      BigDecimal cashParkingDue,
      BigDecimal surchargeDue,
      BigDecimal finalParkingTotal,
      String currency) {}
}
