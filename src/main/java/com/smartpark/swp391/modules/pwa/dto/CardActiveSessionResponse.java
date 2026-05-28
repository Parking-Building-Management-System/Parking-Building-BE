package com.smartpark.swp391.modules.pwa.dto;

import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CardActiveSessionResponse(
    UUID sessionId,
    String plateNumber,
    String licensePlate,
    String cardCode,
    LocalDateTime checkInAt,
    UUID parkingId,
    String parkingName,
    UUID floorId,
    String floorName,
    UUID zoneId,
    String zoneName,
    UUID slotId,
    String slotCode,
    BigDecimal xCoordinate,
    BigDecimal yCoordinate,
    String coordinateMode,
    String mapImageUrl,
    String mapDisplayUrl,
    Long mapUrlExpiresInSeconds,
    ParkingSessionStatus status,
    String guideText) {}
