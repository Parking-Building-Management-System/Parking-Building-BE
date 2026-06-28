package com.smartpark.swp391.modules.staff.dto;

import java.util.Map;
import lombok.Builder;

@Builder
public record ParkingSessionPhotoPresignResponse(
    String uploadUrl,
    String objectKey,
    String method,
    Map<String, String> headers,
    long expiresInSeconds) {}
