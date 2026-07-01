package com.smartpark.swp391.modules.pwa.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PwaReportUploadRequest(
    @NotBlank @Size(max = 255) String fileName, @NotBlank @Size(max = 100) String contentType) {}
