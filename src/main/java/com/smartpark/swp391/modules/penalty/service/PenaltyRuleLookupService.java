package com.smartpark.swp391.modules.penalty.service;

import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import java.util.UUID;

public interface PenaltyRuleLookupService {
  PenaltyRule requireActiveRule(UUID tenantId, UUID parkingId, PenaltyType type);
}
