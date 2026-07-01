package com.smartpark.swp391.modules.staff.dto.lostcard;

import java.util.Map;
import lombok.Builder;

@Builder
public record StaffLostCardPhotoPresignResponse(
    String objectKey,
    String uploadUrl,
    String method,
    Map<String, String> headers,
    long expiresInSeconds,
    String publicUrl) {}
