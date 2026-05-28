package com.smartpark.swp391.modules.manager.dto.map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FloorMapRequest(@NotBlank @Size(max = 1000) String mapImageUrl) {}
