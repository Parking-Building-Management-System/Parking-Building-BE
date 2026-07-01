package com.smartpark.swp391.modules.pwa.dto.report;

import java.util.UUID;
import lombok.Builder;

@Builder
public record OccupiedSlotReportResponse(
    String message,
    UUID oldSlotId,
    String oldSlotCode,
    UUID newSlotId,
    String newSlotCode,
    boolean offenderMatched,
    UUID penaltyCaseId) {}
