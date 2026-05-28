package com.smartpark.swp391.infrastructure.storage.dto;

import java.util.Map;
import lombok.Builder;

@Builder
public record PresignedUpload(
    String objectKey,
    String uploadUrl,
    String method,
    Map<String, String> headers,
    long expiresInSeconds,
    String publicUrl) {}
