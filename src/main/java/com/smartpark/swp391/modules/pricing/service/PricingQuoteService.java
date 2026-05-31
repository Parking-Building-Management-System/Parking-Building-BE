package com.smartpark.swp391.modules.pricing.service;

import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PricingQuoteService {

  PricingQuoteResponse quote(
      UUID tenantId,
      UUID parkingId,
      UUID vehicleTypeId,
      LocalDateTime checkInAt,
      LocalDateTime quotedAt);

  PricingQuoteResponse preview(PricingRule rule, LocalDateTime checkInAt, LocalDateTime quotedAt);
}
