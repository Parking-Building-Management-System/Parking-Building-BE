package com.smartpark.swp391.modules.staff.dto.exit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CompleteExitRequest(
    @NotNull UUID sessionId,
    @NotBlank String cardCode,
    @NotNull ExitPaymentMode paymentMode,
    @NotNull @DecimalMin("0.00") BigDecimal collectedAmount,
    String note) {}
