package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.dto.violation.PendingViolationReportCountResponse;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportApproveRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportRejectRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportResponse;
import com.smartpark.swp391.modules.staff.service.StaffViolationReportService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.LocalDateTime;
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
public class StaffViolationReportServiceImpl implements StaffViolationReportService {

  PenaltyCaseRepository penaltyCaseRepository;
  ParkingSessionRepository parkingSessionRepository;
  PenaltyRuleLookupService penaltyRuleLookupService;
  StaffWorkContextService staffWorkContextService;
  UserRepository userRepository;
  StorageService storageService;

  @Override
  @Transactional(readOnly = true)
  public PendingViolationReportCountResponse getPendingCount() {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    return new PendingViolationReportCountResponse(
        penaltyCaseRepository.countPendingViolationReports(
            context.tenantId(), context.parkingId()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<ViolationReportResponse> getReports(
      PenaltyCaseStatus status, String reportedPlate, LocalDateTime from, LocalDateTime to) {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    return penaltyCaseRepository
        .findViolationReports(
            context.tenantId(), context.parkingId(), status, trimToNull(reportedPlate), from, to)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ViolationReportResponse getReport(UUID id) {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    return toResponse(getReportOrThrow(context, id));
  }

  @Override
  @Transactional
  public ViolationReportResponse approve(UUID id, ViolationReportApproveRequest request) {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    PenaltyCase report = getReportForUpdateOrThrow(context, id);
    requirePending(report);

    ParkingSession offender =
        resolveConfirmedOffender(report, request.confirmedOffenderPlateNumber());
    PenaltyRule rule =
        penaltyRuleLookupService.requireActiveRule(
            context.tenantId(), context.parkingId(), PenaltyType.OCCUPIED_ASSIGNED_SLOT);
    User reviewer = userRepository.getReferenceById(context.staffId());
    LocalDateTime reviewedAt = LocalDateTime.now();

    report.setRule(rule);
    report.setAmount(rule.getAmount());
    report.setCurrency(rule.getCurrency());
    report.setTargetSession(offender);
    report.setOffenderSession(offender);
    report.setTargetLicensePlate(offender.getLicensePlate());
    report.setOffenderLicensePlate(
        normalizePlateForStorage(request.confirmedOffenderPlateNumber()));
    report.setStatus(PenaltyCaseStatus.APPLIED);
    report.setReviewedByStaff(reviewer);
    report.setReviewedAt(reviewedAt);
    report.setReviewNote(trimToNull(request.note()));
    report.setResolvedAt(reviewedAt);
    return toResponse(penaltyCaseRepository.save(report));
  }

  @Override
  @Transactional
  public ViolationReportResponse reject(UUID id, ViolationReportRejectRequest request) {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    PenaltyCase report = getReportForUpdateOrThrow(context, id);
    requirePending(report);

    LocalDateTime reviewedAt = LocalDateTime.now();
    report.setStatus(PenaltyCaseStatus.REJECTED);
    report.setAmount(java.math.BigDecimal.ZERO);
    report.setReviewedByStaff(userRepository.getReferenceById(context.staffId()));
    report.setReviewedAt(reviewedAt);
    report.setReviewNote(request.note().trim());
    report.setResolvedAt(reviewedAt);
    return toResponse(penaltyCaseRepository.save(report));
  }

  private PenaltyCase getReportOrThrow(StaffResolvedContext context, UUID id) {
    return penaltyCaseRepository
        .findViolationReportDetail(context.tenantId(), context.parkingId(), id)
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "VIOLATION_REPORT_NOT_FOUND"));
  }

  private PenaltyCase getReportForUpdateOrThrow(StaffResolvedContext context, UUID id) {
    return penaltyCaseRepository
        .findViolationReportForUpdate(context.tenantId(), context.parkingId(), id)
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "VIOLATION_REPORT_NOT_FOUND"));
  }

  private void requirePending(PenaltyCase report) {
    if (report.getStatus() != PenaltyCaseStatus.REPORTED) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "VIOLATION_REPORT_ALREADY_REVIEWED");
    }
  }

  private ParkingSession resolveConfirmedOffender(PenaltyCase report, String confirmedPlate) {
    String normalizedConfirmedPlate = normalizePlateForCompare(confirmedPlate);
    ParkingSession candidate = report.getOffenderSession();
    if (candidate != null
        && normalizePlateForCompare(candidate.getLicensePlate()).equals(normalizedConfirmedPlate)
        && isValidAtReportTime(candidate, report)) {
      return candidate;
    }

    return parkingSessionRepository
        .findDetailsByTenantIdAndParkingId(report.getTenant().getId(), report.getParking().getId())
        .stream()
        .filter(
            session ->
                normalizePlateForCompare(session.getLicensePlate())
                    .equals(normalizedConfirmedPlate))
        .filter(session -> isValidAtReportTime(session, report))
        .findFirst()
        .orElseThrow(
            () ->
                new ApiException(
                    ErrorCode.INVALID_INPUT,
                    "No matching offender session was active at the report time."));
  }

  private boolean isValidAtReportTime(ParkingSession session, PenaltyCase report) {
    LocalDateTime reportedAt = report.getCreatedAt();
    return session.getCheckInAt() != null
        && !session.getCheckInAt().isAfter(reportedAt)
        && (session.getCheckOutAt() == null || !session.getCheckOutAt().isBefore(reportedAt));
  }

  private ViolationReportResponse toResponse(PenaltyCase report) {
    ParkingSession victim = report.getVictimSession();
    ParkingSession offender = report.getOffenderSession();
    User reviewer = report.getReviewedByStaff();
    return ViolationReportResponse.builder()
        .id(report.getId())
        .status(report.getStatus())
        .reportedAt(report.getCreatedAt())
        .victimPlateNumber(victim == null ? null : victim.getLicensePlate())
        .victimSessionId(victim == null ? null : victim.getId())
        .oldSlotCode(report.getReportedSlot() == null ? null : report.getReportedSlot().getCode())
        .replacementSlotCode(
            report.getReassignedSlot() == null ? null : report.getReassignedSlot().getCode())
        .reportedOffenderPlateNumber(report.getOffenderLicensePlate())
        .offenderSessionId(offender == null ? null : offender.getId())
        .matchedOffenderPlateNumber(offender == null ? null : offender.getLicensePlate())
        .evidenceImageUrl(resolveDisplayUrl(report.getEvidenceImageUrl()))
        .victimEntryImageUrl(victim == null ? null : resolveDisplayUrl(victim.getEntryImageUrl()))
        .victimLicensePlateImageUrl(
            victim == null ? null : resolveDisplayUrl(victim.getLicensePlateImageUrl()))
        .offenderEntryImageUrl(
            offender == null ? null : resolveDisplayUrl(offender.getEntryImageUrl()))
        .offenderLicensePlateImageUrl(
            offender == null ? null : resolveDisplayUrl(offender.getLicensePlateImageUrl()))
        .reportNote(report.getNote())
        .reviewedByStaffId(reviewer == null ? null : reviewer.getId())
        .reviewedByStaffName(reviewer == null ? null : reviewer.getFullName())
        .reviewedAt(report.getReviewedAt())
        .reviewNote(report.getReviewNote())
        .appliedAmount(report.getStatus() == PenaltyCaseStatus.APPLIED ? report.getAmount() : null)
        .currency(report.getCurrency())
        .build();
  }

  private String resolveDisplayUrl(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return null;
    }
    if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
      return objectKey;
    }
    PresignedDownload download =
        storageService.createPresignedDownload(currentTenantId(), objectKey);
    return download.downloadUrl();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private String normalizePlateForStorage(String value) {
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizePlateForCompare(String value) {
    return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
