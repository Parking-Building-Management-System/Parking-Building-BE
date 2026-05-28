package com.smartpark.swp391.modules.manager.dto.map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SlotCoordinateRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal xCoordinate,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal yCoordinate) {}
