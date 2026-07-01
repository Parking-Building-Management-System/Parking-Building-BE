package com.smartpark.swp391.modules.penalty.dto;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PenaltyCaseResponse(
    UUID id,
    PenaltyType type,
    String name,
    BigDecimal amount,
    String currency,
    PenaltyCaseStatus status,
    UUID targetSessionId,
    UUID victimSessionId,
    UUID offenderSessionId,
    String targetLicensePlate,
    String offenderLicensePlate,
    UUID reportedSlotId,
    String reportedSlotCode,
    UUID reassignedSlotId,
    String reassignedSlotCode,
    String evidenceImageUrl,
    String identityImageUrl,
    String vehicleImageUrl,
    String licensePlateImageUrl,
    String note,
    LocalDateTime createdAt,
    LocalDateTime collectedAt) {}
