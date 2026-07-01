package com.smartpark.swp391.modules.penalty.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyRuleRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
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
public class PenaltyRuleLookupServiceImpl implements PenaltyRuleLookupService {

  PenaltyRuleRepository penaltyRuleRepository;

  @Override
  @Transactional(readOnly = true)
  public PenaltyRule requireActiveRule(UUID tenantId, UUID parkingId, PenaltyType type) {
    List<PenaltyRule> parkingRules =
        penaltyRuleRepository.findActiveParkingRules(tenantId, parkingId, type);
    if (parkingRules.size() > 1) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "MULTIPLE_PENALTY_RULES_MATCH");
    }
    if (!parkingRules.isEmpty()) {
      return parkingRules.getFirst();
    }

    List<PenaltyRule> defaultRules =
        penaltyRuleRepository.findActiveTenantDefaultRules(tenantId, type);
    if (defaultRules.size() > 1) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "MULTIPLE_PENALTY_RULES_MATCH");
    }
    if (!defaultRules.isEmpty()) {
      return defaultRules.getFirst();
    }

    throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PENALTY_RULE_NOT_CONFIGURED");
  }
}
