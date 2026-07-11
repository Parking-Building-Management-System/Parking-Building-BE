package com.smartpark.swp391.modules.staff.dto.violation;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ViolationReportResponse(
    UUID id,
    PenaltyCaseStatus status,
    LocalDateTime reportedAt,
    String victimPlateNumber,
    UUID victimSessionId,
    String oldSlotCode,
    String replacementSlotCode,
    String reportedOffenderPlateNumber,
    UUID offenderSessionId,
    String matchedOffenderPlateNumber,
    String evidenceImageUrl,
    String victimEntryImageUrl,
    String victimLicensePlateImageUrl,
    String offenderEntryImageUrl,
    String offenderLicensePlateImageUrl,
    String reportNote,
    UUID reviewedByStaffId,
    String reviewedByStaffName,
    LocalDateTime reviewedAt,
    String reviewNote,
    BigDecimal appliedAmount,
    String currency) {}
