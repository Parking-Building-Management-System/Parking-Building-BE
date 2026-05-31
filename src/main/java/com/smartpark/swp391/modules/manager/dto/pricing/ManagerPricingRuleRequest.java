package com.smartpark.swp391.modules.manager.dto.pricing;

import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ManagerPricingRuleRequest(
    @NotBlank @Size(max = 120) String name,
    UUID parkingId,
    @NotNull UUID vehicleTypeId,
    @NotNull @Min(0) Integer freeMinutes,
    @NotNull @Min(1) Integer firstBlockMinutes,
    @NotNull @DecimalMin("0.00") BigDecimal firstBlockPrice,
    @NotNull @Min(1) Integer nextBlockMinutes,
    @NotNull @DecimalMin("0.00") BigDecimal nextBlockPrice,
    @DecimalMin("0.00") BigDecimal dailyCapPrice,
    @NotNull @Min(0) Integer graceMinutesAfterPayment,
    PricingRuleStatus status) {}
