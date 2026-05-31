package com.smartpark.swp391.modules.manager.dto.pricing;

import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerPricingRuleResponse(
    UUID id,
    String name,
    UUID parkingId,
    String parkingName,
    UUID vehicleTypeId,
    String vehicleTypeCode,
    String vehicleTypeName,
    Integer freeMinutes,
    Integer firstBlockMinutes,
    BigDecimal firstBlockPrice,
    Integer nextBlockMinutes,
    BigDecimal nextBlockPrice,
    BigDecimal dailyCapPrice,
    Integer graceMinutesAfterPayment,
    PricingRuleStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
