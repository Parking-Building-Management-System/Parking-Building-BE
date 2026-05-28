package com.smartpark.swp391.modules.manager.dto.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignUploadRequest(
    @NotBlank @Size(max = 255) String fileName,
    @NotBlank @Size(max = 100) String contentType,
    @Size(max = 120) String folder) {}
