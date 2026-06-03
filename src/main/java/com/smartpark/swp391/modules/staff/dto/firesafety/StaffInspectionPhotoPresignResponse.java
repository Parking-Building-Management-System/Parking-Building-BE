package com.smartpark.swp391.modules.staff.dto.firesafety;

import java.util.Map;
import lombok.Builder;

@Builder
public record StaffInspectionPhotoPresignResponse(
    String uploadUrl,
    String objectKey,
    String method,
    Map<String, String> headers,
    long expiresInSeconds) {}
