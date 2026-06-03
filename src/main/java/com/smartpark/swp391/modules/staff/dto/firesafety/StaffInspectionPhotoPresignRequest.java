package com.smartpark.swp391.modules.staff.dto.firesafety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffInspectionPhotoPresignRequest(
    @NotBlank @Size(max = 255) String fileName, @NotBlank @Size(max = 100) String contentType) {}
