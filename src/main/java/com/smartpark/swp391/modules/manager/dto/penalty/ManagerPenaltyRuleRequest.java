package com.smartpark.swp391.modules.manager.dto.penalty;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ManagerPenaltyRuleRequest(
    @Size(max = 60) String code,
    @NotBlank @Size(max = 120) String name,
    UUID parkingId,
    @NotNull PenaltyType type,
    @NotNull @DecimalMin("0.00") BigDecimal amount,
    @Size(max = 10) String currency,
    Boolean requiresPhoto,
    @Size(max = 1000) String description,
    PenaltyRuleStatus status) {}
