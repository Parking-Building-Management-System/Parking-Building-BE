package com.smartpark.swp391.modules.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehicleTypeCreateRequest(
    @NotBlank(message = "Tên loại phương tiện không được để trống")
        @Size(max = 255, message = "Tên loại phương tiện không được vượt quá 255 ký tự")
        String name,
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự") String description) {}
