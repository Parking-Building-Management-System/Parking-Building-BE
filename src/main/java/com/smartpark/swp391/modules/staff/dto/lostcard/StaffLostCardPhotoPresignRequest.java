package com.smartpark.swp391.modules.staff.dto.lostcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffLostCardPhotoPresignRequest(
    @NotBlank @Size(max = 255) String fileName,
    @NotBlank @Size(max = 100) String contentType,
    @NotBlank @Size(max = 40) String photoType) {}
