package com.smartpark.swp391.modules.pwa.dto;

import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.pricing.dto.PricingBreakdownItemResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CardCheckoutQuoteResponse(
    UUID sessionId,
    String plateNumber,
    String licensePlate,
    String cardCode,
    ParkingSessionStatus status,
    LocalDateTime checkInAt,
    LocalDateTime quotedAt,
    long durationMinutes,
    long chargeableMinutes,
    UUID vehicleTypeId,
    String vehicleTypeName,
    String parkingName,
    String floorName,
    String zoneName,
    String slotCode,
    BigDecimal amount,
    String currency,
    UUID pricingRuleId,
    String pricingRuleName,
    List<PricingBreakdownItemResponse> pricingBreakdown,
    boolean paymentAvailable,
    String nextAction) {}
