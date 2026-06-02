package com.smartpark.swp391.modules.manager.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import jakarta.validation.constraints.NotNull;

public record FireExtinguisherStatusRequest(@NotNull FireExtinguisherStatus status, String note) {}
