package com.smartpark.swp391.modules.manager.dto.kiosk;

import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerKioskStatusRequest(@NotNull KioskStatus status) {}
