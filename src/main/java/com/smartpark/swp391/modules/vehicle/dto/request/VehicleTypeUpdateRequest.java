package com.smartpark.swp391.modules.vehicle.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleTypeUpdateRequest(
    @Size(max = 100, message = "Tên loại phương tiện tối đa 100 ký tự")
        @Pattern(regexp = ".*\\S.*", message = "Tên loại phương tiện không được để trống")
        String name,
    @Size(max = 50, message = "Mã loại phương tiện tối đa 50 ký tự")
        @Pattern(
            regexp = "^[A-Z0-9_]+$",
            message = "Mã loại phương tiện chỉ gồm chữ hoa, số và dấu gạch dưới")
        String code,
    Boolean active) {}
