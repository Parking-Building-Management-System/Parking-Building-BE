package com.smartpark.swp391.modules.manager.dto.slot;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SlotRequest(
    @NotBlank @Size(max = 50) String code,
    @Size(max = 50) String slotNumber,
    SlotStatus status) {}
