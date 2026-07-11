package com.smartpark.swp391.modules.staff.dto.violation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ViolationReportApproveRequest(
    @NotBlank @Size(max = 30) String confirmedOffenderPlateNumber, @Size(max = 1000) String note) {}
