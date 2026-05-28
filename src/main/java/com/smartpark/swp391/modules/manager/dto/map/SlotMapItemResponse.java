package com.smartpark.swp391.modules.manager.dto.map;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SlotMapItemResponse(
    UUID slotId,
    String slotCode,
    UUID zoneId,
    String zoneName,
    SlotStatus status,
    BigDecimal xCoordinate,
    BigDecimal yCoordinate,
    boolean hasCoordinate) {}
