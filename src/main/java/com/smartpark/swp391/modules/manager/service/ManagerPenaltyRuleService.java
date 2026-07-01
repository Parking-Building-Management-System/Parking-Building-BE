package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleRequest;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleResponse;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleStatusRequest;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import java.util.UUID;

public interface ManagerPenaltyRuleService {

  PageResponse<ManagerPenaltyRuleResponse> getRules(
      UUID parkingId, PenaltyType type, PenaltyRuleStatus status, int page, int size);

  ManagerPenaltyRuleResponse createRule(ManagerPenaltyRuleRequest request);

  ManagerPenaltyRuleResponse getRule(UUID id);

  ManagerPenaltyRuleResponse updateRule(UUID id, ManagerPenaltyRuleRequest request);

  ManagerPenaltyRuleResponse updateStatus(UUID id, ManagerPenaltyRuleStatusRequest request);

  void deleteRule(UUID id);
}
