package com.smartpark.swp391.modules.pricing.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PricingBreakdownItemResponse(
    String label, long minutes, BigDecimal unitPrice, BigDecimal amount) {}
