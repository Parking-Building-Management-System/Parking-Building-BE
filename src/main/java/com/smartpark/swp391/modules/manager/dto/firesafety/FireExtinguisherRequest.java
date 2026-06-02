package com.smartpark.swp391.modules.manager.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FireExtinguisherRequest(
    @NotNull UUID parkingId,
    @NotNull UUID floorId,
    UUID zoneId,
    @NotBlank @Size(max = 100) String code,
    @NotNull FireExtinguisherType type,
    String locationDescription,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal xCoordinate,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal yCoordinate,
    LocalDate manufactureDate,
    LocalDate expiryDate,
    LocalDateTime nextInspectionAt,
    FireExtinguisherStatus status,
    String note) {}
