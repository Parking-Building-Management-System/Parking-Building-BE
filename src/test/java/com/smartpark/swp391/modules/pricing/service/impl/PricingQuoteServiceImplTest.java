package com.smartpark.swp391.modules.pricing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingQuoteServiceImplTest {

  @Mock PricingRuleRepository pricingRuleRepository;

  @Test
  void freeMinutesProduceZeroAmount() {
    var quote =
        service()
            .preview(
                rule("Free", 15, 60, "20000", 30, "10000"),
                LocalDateTime.parse("2026-05-28T10:00:00"),
                LocalDateTime.parse("2026-05-28T10:10:00"));

    assertThat(quote.durationMinutes()).isEqualTo(10);
    assertThat(quote.chargeableMinutes()).isZero();
    assertThat(quote.amount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void firstBlockOnlyChargesFirstBlockPrice() {
    var quote =
        service()
            .preview(
                rule("First", 10, 120, "20000", 60, "10000"),
                LocalDateTime.parse("2026-05-28T10:00:00"),
                LocalDateTime.parse("2026-05-28T11:30:00"));

    assertThat(quote.durationMinutes()).isEqualTo(90);
    assertThat(quote.chargeableMinutes()).isEqualTo(80);
    assertThat(quote.amount()).isEqualByComparingTo("20000");
  }

  @Test
  void additionalBlocksRoundUp() {
    var quote =
        service()
            .preview(
                rule("Additional", 0, 120, "20000", 60, "10000"),
                LocalDateTime.parse("2026-05-28T10:00:00"),
                LocalDateTime.parse("2026-05-28T12:35:01"));

    assertThat(quote.durationMinutes()).isEqualTo(156);
    assertThat(quote.chargeableMinutes()).isEqualTo(156);
    assertThat(quote.amount()).isEqualByComparingTo("30000");
    assertThat(quote.pricingBreakdown()).extracting("label").contains("Additional block x 1");
  }

  @Test
  void oneMinuteChargesTheStartedFirstBlock() {
    var quote =
        service()
            .preview(
                rule("One minute", 0, 60, "5000", 60, "5000"),
                LocalDateTime.parse("2026-05-28T10:00:00"),
                LocalDateTime.parse("2026-05-28T10:01:00"));

    assertThat(quote.amount()).isEqualByComparingTo("5000");
  }

  @Test
  void longDurationChargesEveryStartedBlock() {
    var quote =
        service()
            .preview(
                rule("Long stay", 0, 60, "5000", 60, "5000"),
                LocalDateTime.parse("2026-05-28T00:00:00"),
                LocalDateTime.parse("2026-05-28T15:00:00"));

    assertThat(quote.durationMinutes()).isEqualTo(900);
    assertThat(quote.amount()).isEqualByComparingTo("75000");
    assertThat(quote.pricingBreakdown())
        .extracting("label")
        .containsExactly("First block", "Additional block x 14");
  }

  @Test
  void hourlyPricingExamplesUseStartedBlockRoundingWithoutMaximum() {
    PricingRule rule = rule("Hourly", 0, 60, "5000", 60, "5000");

    assertThat(amountForMinutes(rule, 1)).isEqualByComparingTo("5000");
    assertThat(amountForMinutes(rule, 60)).isEqualByComparingTo("5000");
    assertThat(amountForMinutes(rule, 61)).isEqualByComparingTo("10000");
    assertThat(amountForMinutes(rule, 180)).isEqualByComparingTo("15000");
    assertThat(amountForMinutes(rule, 900)).isEqualByComparingTo("75000");
    assertThat(amountForMinutes(rule, 1440)).isEqualByComparingTo("120000");
  }

  @Test
  void multiDayDurationChargesEveryStartedBlock() {
    var quote =
        service()
            .preview(
                rule("Multi-day stay", 0, 60, "5000", 60, "5000"),
                LocalDateTime.parse("2026-05-28T00:00:00"),
                LocalDateTime.parse("2026-05-30T00:01:00"));

    assertThat(quote.durationMinutes()).isEqualTo(2881);
    assertThat(quote.amount()).isEqualByComparingTo("245000");
  }

  @Test
  void zeroPriceRuleRemainsFree() {
    var quote =
        service()
            .preview(
                rule("Zero price", 0, 60, "0", 60, "0"),
                LocalDateTime.parse("2026-05-28T00:00:00"),
                LocalDateTime.parse("2026-05-30T00:01:00"));

    assertThat(quote.amount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void noPricingRuleFailsClearly() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    UUID vehicleTypeId = UUID.randomUUID();
    when(pricingRuleRepository.findActiveParkingRules(tenantId, parkingId, vehicleTypeId))
        .thenReturn(List.of());
    when(pricingRuleRepository.findActiveTenantDefaultRules(tenantId, vehicleTypeId))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service()
                    .quote(
                        tenantId,
                        parkingId,
                        vehicleTypeId,
                        LocalDateTime.parse("2026-05-28T10:00:00"),
                        LocalDateTime.parse("2026-05-28T11:00:00")))
        .isInstanceOf(ApiException.class)
        .hasMessage("PRICING_RULE_NOT_CONFIGURED");
  }

  @Test
  void parkingSpecificRuleBeatsTenantDefaultRule() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    UUID vehicleTypeId = UUID.randomUUID();
    PricingRule parkingRule = rule("Parking specific", 0, 60, "10000", 60, "10000");
    PricingRule defaultRule = rule("Default", 0, 60, "50000", 60, "50000");
    when(pricingRuleRepository.findActiveParkingRules(tenantId, parkingId, vehicleTypeId))
        .thenReturn(List.of(parkingRule));

    var quote =
        service()
            .quote(
                tenantId,
                parkingId,
                vehicleTypeId,
                LocalDateTime.parse("2026-05-28T10:00:00"),
                LocalDateTime.parse("2026-05-28T11:00:00"));

    assertThat(quote.pricingRuleName()).isEqualTo(parkingRule.getName());
    assertThat(quote.amount()).isEqualByComparingTo("10000");
    assertThat(defaultRule.getName()).isEqualTo("Default");
  }

  private PricingQuoteServiceImpl service() {
    return new PricingQuoteServiceImpl(pricingRuleRepository);
  }

  private BigDecimal amountForMinutes(PricingRule rule, long minutes) {
    LocalDateTime checkInAt = LocalDateTime.parse("2026-05-28T00:00:00");
    return service().preview(rule, checkInAt, checkInAt.plusMinutes(minutes)).amount();
  }

  private PricingRule rule(
      String name,
      int freeMinutes,
      int firstBlockMinutes,
      String firstBlockPrice,
      int nextBlockMinutes,
      String nextBlockPrice) {
    PricingRule rule =
        PricingRule.builder()
            .name(name)
            .freeMinutes(freeMinutes)
            .firstBlockMinutes(firstBlockMinutes)
            .firstBlockPrice(new BigDecimal(firstBlockPrice))
            .nextBlockMinutes(nextBlockMinutes)
            .nextBlockPrice(new BigDecimal(nextBlockPrice))
            .graceMinutesAfterPayment(15)
            .build();
    rule.setId(UUID.randomUUID());
    return rule;
  }
}
