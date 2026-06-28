package com.smartpark.swp391.modules.staff.dto;

import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ParkingSessionCheckInResponse(
    UUID sessionId,
    String plateNumber,
    String cardCode,
    String qrToken,
    String pwaAccessPath,
    UUID assignedSlotId,
    String assignedSlotCode,
    UUID zoneId,
    String zoneName,
    UUID vehicleTypeId,
    String vehicleTypeCode,
    String vehicleTypeName,
    UUID parkingId,
    LocalDateTime entryTime,
    ParkingSessionStatus status) {}
