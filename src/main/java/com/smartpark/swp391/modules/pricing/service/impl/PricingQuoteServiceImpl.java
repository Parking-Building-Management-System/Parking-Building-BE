package com.smartpark.swp391.modules.pricing.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.pricing.dto.PricingBreakdownItemResponse;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PricingQuoteServiceImpl implements PricingQuoteService {

  private static final String CURRENCY = "VND";

  PricingRuleRepository pricingRuleRepository;

  @Override
  @Transactional(readOnly = true)
  public PricingQuoteResponse quote(
      UUID tenantId,
      UUID parkingId,
      UUID vehicleTypeId,
      LocalDateTime checkInAt,
      LocalDateTime quotedAt) {
    PricingRule rule = resolveRule(tenantId, parkingId, vehicleTypeId);
    return calculate(rule, checkInAt, quotedAt);
  }

  @Override
  public PricingQuoteResponse preview(
      PricingRule rule, LocalDateTime checkInAt, LocalDateTime quotedAt) {
    return calculate(rule, checkInAt, quotedAt);
  }

  private PricingRule resolveRule(UUID tenantId, UUID parkingId, UUID vehicleTypeId) {
    List<PricingRule> parkingRules =
        pricingRuleRepository.findActiveParkingRules(tenantId, parkingId, vehicleTypeId);
    if (parkingRules.size() > 1) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "MULTIPLE_PRICING_RULES_MATCH");
    }
    if (!parkingRules.isEmpty()) {
      return parkingRules.getFirst();
    }

    List<PricingRule> defaultRules =
        pricingRuleRepository.findActiveTenantDefaultRules(tenantId, vehicleTypeId);
    if (defaultRules.size() > 1) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "MULTIPLE_PRICING_RULES_MATCH");
    }
    if (!defaultRules.isEmpty()) {
      return defaultRules.getFirst();
    }

    throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PRICING_RULE_NOT_CONFIGURED");
  }

  private PricingQuoteResponse calculate(
      PricingRule rule, LocalDateTime checkInAt, LocalDateTime quotedAt) {
    requireValidRule(rule);
    if (checkInAt == null || quotedAt == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Quote timestamps are required");
    }

    long durationMinutes = ceilMinutes(checkInAt, quotedAt);
    long freeApplied = Math.min(durationMinutes, rule.getFreeMinutes());
    long chargeableMinutes = Math.max(0, durationMinutes - rule.getFreeMinutes());
    List<PricingBreakdownItemResponse> breakdown = new ArrayList<>();

    if (rule.getFreeMinutes() > 0) {
      breakdown.add(
          PricingBreakdownItemResponse.builder()
              .label("Free minutes")
              .minutes(freeApplied)
              .unitPrice(BigDecimal.ZERO)
              .amount(BigDecimal.ZERO)
              .build());
    }

    BigDecimal amount = BigDecimal.ZERO;
    long remaining = chargeableMinutes;
    if (remaining > 0) {
      long firstBlockMinutes = Math.min(remaining, rule.getFirstBlockMinutes());
      amount = amount.add(rule.getFirstBlockPrice());
      breakdown.add(
          PricingBreakdownItemResponse.builder()
              .label("First block")
              .minutes(firstBlockMinutes)
              .unitPrice(rule.getFirstBlockPrice())
              .amount(rule.getFirstBlockPrice())
              .build());
      remaining -= firstBlockMinutes;
    }

    if (remaining > 0) {
      long blocks = ceilDiv(remaining, rule.getNextBlockMinutes());
      BigDecimal additionalAmount = rule.getNextBlockPrice().multiply(BigDecimal.valueOf(blocks));
      amount = amount.add(additionalAmount);
      breakdown.add(
          PricingBreakdownItemResponse.builder()
              .label("Additional block x " + blocks)
              .minutes(remaining)
              .unitPrice(rule.getNextBlockPrice())
              .amount(additionalAmount)
              .build());
    }

    if (rule.getDailyCapPrice() != null && amount.compareTo(rule.getDailyCapPrice()) > 0) {
      BigDecimal adjustment = rule.getDailyCapPrice().subtract(amount);
      amount = rule.getDailyCapPrice();
      breakdown.add(
          PricingBreakdownItemResponse.builder()
              .label("Daily cap applied")
              .minutes(durationMinutes)
              .unitPrice(rule.getDailyCapPrice())
              .amount(adjustment)
              .build());
    }

    return PricingQuoteResponse.builder()
        .pricingRuleId(rule.getId())
        .pricingRuleName(rule.getName())
        .checkInAt(checkInAt)
        .quotedAt(quotedAt)
        .durationMinutes(durationMinutes)
        .chargeableMinutes(chargeableMinutes)
        .amount(amount)
        .currency(CURRENCY)
        .pricingBreakdown(breakdown)
        .build();
  }

  private long ceilMinutes(LocalDateTime start, LocalDateTime end) {
    long seconds = Math.max(0, Duration.between(start, end).getSeconds());
    return ceilDiv(seconds, 60);
  }

  private long ceilDiv(long value, long divisor) {
    if (value <= 0) {
      return 0;
    }
    return (value + divisor - 1) / divisor;
  }

  private void requireValidRule(PricingRule rule) {
    if (rule == null) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PRICING_RULE_NOT_CONFIGURED");
    }
    if (rule.getFreeMinutes() == null || rule.getFreeMinutes() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "freeMinutes must be >= 0");
    }
    if (rule.getFirstBlockMinutes() == null || rule.getFirstBlockMinutes() <= 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "firstBlockMinutes must be > 0");
    }
    if (rule.getFirstBlockPrice() == null || rule.getFirstBlockPrice().signum() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "firstBlockPrice must be >= 0");
    }
    if (rule.getNextBlockMinutes() == null || rule.getNextBlockMinutes() <= 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "nextBlockMinutes must be > 0");
    }
    if (rule.getNextBlockPrice() == null || rule.getNextBlockPrice().signum() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "nextBlockPrice must be >= 0");
    }
    if (rule.getDailyCapPrice() != null && rule.getDailyCapPrice().signum() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "dailyCapPrice must be >= 0");
    }
    if (rule.getGraceMinutesAfterPayment() == null || rule.getGraceMinutesAfterPayment() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "graceMinutesAfterPayment must be >= 0");
    }
  }
}
