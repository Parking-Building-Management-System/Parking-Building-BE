package com.smartpark.swp391.modules.manager.dto.kiosk;

import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ManagerKioskRequest(
    @NotNull UUID parkingId,
    @NotBlank @Size(max = 100) String name,
    @NotNull KioskType type,
    @NotNull KioskStatus status) {}
