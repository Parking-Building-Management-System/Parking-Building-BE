package com.smartpark.swp391.modules.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PricingQuoteResponse(
    UUID pricingRuleId,
    String pricingRuleName,
    LocalDateTime checkInAt,
    LocalDateTime quotedAt,
    long durationMinutes,
    long chargeableMinutes,
    BigDecimal amount,
    String currency,
    List<PricingBreakdownItemResponse> pricingBreakdown) {}
