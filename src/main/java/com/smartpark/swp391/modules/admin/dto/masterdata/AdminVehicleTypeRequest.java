package com.smartpark.swp391.modules.admin.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminVehicleTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String code,
    Boolean active) {}
