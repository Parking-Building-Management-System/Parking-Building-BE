package com.smartpark.swp391.modules.staff.dto.violation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ViolationReportRejectRequest(@NotBlank @Size(max = 1000) String note) {}
