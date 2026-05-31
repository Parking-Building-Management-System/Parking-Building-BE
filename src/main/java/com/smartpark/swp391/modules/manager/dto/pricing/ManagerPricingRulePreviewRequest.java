package com.smartpark.swp391.modules.manager.dto.pricing;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ManagerPricingRulePreviewRequest(
    @NotNull OffsetDateTime checkInAt, @NotNull OffsetDateTime checkOutAt, UUID vehicleTypeId) {}
