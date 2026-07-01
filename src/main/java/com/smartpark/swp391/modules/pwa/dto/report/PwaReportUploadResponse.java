package com.smartpark.swp391.modules.pwa.dto.report;

import java.util.Map;
import lombok.Builder;

@Builder
public record PwaReportUploadResponse(
    String objectKey,
    String uploadUrl,
    String method,
    Map<String, String> headers,
    long expiresInSeconds,
    String publicUrl) {}
