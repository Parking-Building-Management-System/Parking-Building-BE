package com.smartpark.swp391.modules.manager.dto.penalty;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerPenaltyRuleStatusRequest(@NotNull PenaltyRuleStatus status) {}
