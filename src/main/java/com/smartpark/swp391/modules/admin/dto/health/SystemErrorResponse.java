package com.smartpark.swp391.modules.admin.dto.health;

import java.time.Instant;
import lombok.Builder;

@Builder
public record SystemErrorResponse(
    Instant timestamp,
    String method,
    String path,
    int status,
    String errorCode,
    String message,
    long count) {}
