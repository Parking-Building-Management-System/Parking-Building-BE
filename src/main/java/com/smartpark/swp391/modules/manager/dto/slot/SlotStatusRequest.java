package com.smartpark.swp391.modules.manager.dto.slot;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import jakarta.validation.constraints.NotNull;

public record SlotStatusRequest(@NotNull SlotStatus status) {}
