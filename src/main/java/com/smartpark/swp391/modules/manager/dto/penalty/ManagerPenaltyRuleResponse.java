package com.smartpark.swp391.modules.manager.dto.penalty;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerPenaltyRuleResponse(
    UUID id,
    String code,
    String name,
    UUID parkingId,
    String parkingName,
    PenaltyType type,
    BigDecimal amount,
    String currency,
    boolean requiresPhoto,
    String description,
    PenaltyRuleStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
