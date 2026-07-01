package com.smartpark.swp391.modules.staff.dto.lostcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record StaffLostCardCompleteExitRequest(
    @NotNull UUID sessionId,
    @NotNull UUID lostCardCaseId,
    @NotNull @DecimalMin("0.00") BigDecimal collectedAmount,
    @Size(max = 1000) String note) {}
