package com.smartpark.swp391.modules.pwa.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OccupiedSlotReportRequest(
    @NotBlank @Size(max = 30) String offenderPlateNumber,
    @NotBlank @Size(max = 1000) String evidenceImageUrl,
    @Size(max = 1000) String note) {}
