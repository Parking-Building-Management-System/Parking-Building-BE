package com.smartpark.swp391.modules.penalty.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PenaltyRuleLookupServiceImplTest {

  @Mock PenaltyRuleRepository penaltyRuleRepository;

  @Test
  void requireActiveRulePrefersParkingRuleOverTenantDefault() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    PenaltyRule parkingRule = rule(PenaltyType.OCCUPIED_ASSIGNED_SLOT, "50000");
    when(penaltyRuleRepository.findActiveParkingRules(
            tenantId, parkingId, PenaltyType.OCCUPIED_ASSIGNED_SLOT))
        .thenReturn(List.of(parkingRule));

    PenaltyRule result =
        service().requireActiveRule(tenantId, parkingId, PenaltyType.OCCUPIED_ASSIGNED_SLOT);

    assertThat(result).isEqualTo(parkingRule);
  }

  @Test
  void requireActiveRuleFallsBackToTenantDefault() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    PenaltyRule defaultRule = rule(PenaltyType.LOST_CARD, "100000");
    when(penaltyRuleRepository.findActiveParkingRules(tenantId, parkingId, PenaltyType.LOST_CARD))
        .thenReturn(List.of());
    when(penaltyRuleRepository.findActiveTenantDefaultRules(tenantId, PenaltyType.LOST_CARD))
        .thenReturn(List.of(defaultRule));

    PenaltyRule result = service().requireActiveRule(tenantId, parkingId, PenaltyType.LOST_CARD);

    assertThat(result).isEqualTo(defaultRule);
  }

  @Test
  void requireActiveRuleThrowsClearErrorWhenMissing() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    when(penaltyRuleRepository.findActiveParkingRules(tenantId, parkingId, PenaltyType.LOST_CARD))
        .thenReturn(List.of());
    when(penaltyRuleRepository.findActiveTenantDefaultRules(tenantId, PenaltyType.LOST_CARD))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service().requireActiveRule(tenantId, parkingId, PenaltyType.LOST_CARD))
        .isInstanceOf(ApiException.class)
        .hasMessage("PENALTY_RULE_NOT_CONFIGURED");
  }

  private PenaltyRuleLookupServiceImpl service() {
    return new PenaltyRuleLookupServiceImpl(penaltyRuleRepository);
  }

  private PenaltyRule rule(PenaltyType type, String amount) {
    PenaltyRule rule =
        PenaltyRule.builder()
            .code(type.name())
            .name(type.name())
            .type(type)
            .amount(new BigDecimal(amount))
            .currency("VND")
            .status(PenaltyRuleStatus.ACTIVE)
            .build();
    rule.setId(UUID.randomUUID());
    return rule;
  }
}
