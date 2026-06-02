package com.smartpark.swp391.modules.manager.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FireExtinguisherResponse(
    UUID id,
    UUID parkingId,
    String parkingName,
    UUID floorId,
    String floorName,
    String floorCode,
    UUID zoneId,
    String zoneName,
    String code,
    FireExtinguisherType type,
    String locationDescription,
    BigDecimal xCoordinate,
    BigDecimal yCoordinate,
    Boolean hasCoordinate,
    LocalDate manufactureDate,
    LocalDate expiryDate,
    LocalDateTime lastInspectedAt,
    LocalDateTime nextInspectionAt,
    FireExtinguisherStatus status,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
