package com.smartpark.swp391.modules.manager.dto.storage;

import java.util.Map;
import lombok.Builder;

@Builder
public record PresignUploadResponse(
    String objectKey,
    String uploadUrl,
    String method,
    Map<String, String> headers,
    long expiresInSeconds,
    String publicUrl) {}
