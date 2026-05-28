package com.smartpark.swp391.modules.manager.dto.map;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SlotCoordinateResponse(
    UUID slotId, BigDecimal xCoordinate, BigDecimal yCoordinate, boolean hasCoordinate) {}
