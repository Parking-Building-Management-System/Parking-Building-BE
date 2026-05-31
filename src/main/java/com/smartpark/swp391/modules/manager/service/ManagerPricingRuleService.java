package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRulePreviewRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleResponse;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleStatusRequest;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import java.util.UUID;

public interface ManagerPricingRuleService {

  PageResponse<ManagerPricingRuleResponse> getRules(
      UUID parkingId, UUID vehicleTypeId, PricingRuleStatus status, int page, int size);

  ManagerPricingRuleResponse createRule(ManagerPricingRuleRequest request);

  ManagerPricingRuleResponse getRule(UUID id);

  ManagerPricingRuleResponse updateRule(UUID id, ManagerPricingRuleRequest request);

  ManagerPricingRuleResponse updateStatus(UUID id, ManagerPricingRuleStatusRequest request);

  void deleteRule(UUID id);

  PricingQuoteResponse preview(UUID id, ManagerPricingRulePreviewRequest request);
}
