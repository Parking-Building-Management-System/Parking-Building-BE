package com.smartpark.swp391.modules.admin.dto.health;

import lombok.Builder;

@Builder
public record TopEndpointResponse(
    String method, String path, long requestCount, long errorCount, long avgLatencyMs) {}
