package com.smartpark.swp391.modules.manager.dto.facility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FloorRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull Integer displayOrder,
    Boolean active) {}
