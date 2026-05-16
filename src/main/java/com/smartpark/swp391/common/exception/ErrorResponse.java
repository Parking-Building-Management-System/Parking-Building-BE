package com.smartpark.swp391.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String code,
    String path,
    Map<String, String> fieldErrors) {}
