package com.smartpark.swp391.modules.manager.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FireSafetyMapItemResponse(
    UUID id,
    String code,
    FireExtinguisherType type,
    FireExtinguisherStatus status,
    UUID zoneId,
    String zoneName,
    String locationDescription,
    BigDecimal xCoordinate,
    BigDecimal yCoordinate,
    LocalDate expiryDate,
    LocalDateTime nextInspectionAt,
    boolean hasCoordinate) {}
