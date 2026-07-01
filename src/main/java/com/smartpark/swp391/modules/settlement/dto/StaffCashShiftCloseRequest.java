package com.smartpark.swp391.modules.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StaffCashShiftCloseRequest(
    @NotNull @DecimalMin("0.00") BigDecimal countedCashAmount, @Size(max = 1000) String note) {}
