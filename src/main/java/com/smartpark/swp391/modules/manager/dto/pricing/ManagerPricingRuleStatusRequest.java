package com.smartpark.swp391.modules.manager.dto.pricing;

import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerPricingRuleStatusRequest(@NotNull PricingRuleStatus status) {}
