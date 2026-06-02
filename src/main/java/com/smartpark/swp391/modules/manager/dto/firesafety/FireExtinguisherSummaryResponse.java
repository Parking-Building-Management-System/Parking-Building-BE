package com.smartpark.swp391.modules.manager.dto.firesafety;

import lombok.Builder;

@Builder
public record FireExtinguisherSummaryResponse(
    long total,
    long active,
    long expired,
    long missing,
    long damaged,
    long maintenance,
    long dueInspection,
    long expiringSoon) {}
