package com.smartpark.swp391.modules.manager.dto.rfid;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RfidCardGenerateRequest(
    @Min(1) @Max(10000) Integer count, @Size(max = 30) String prefix) {}
