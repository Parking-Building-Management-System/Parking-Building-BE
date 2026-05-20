package com.smartpark.swp391.modules.manager.dto.facility;

import com.smartpark.swp391.modules.parking.enumType.ZoneStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ZoneResponse(
    UUID id,
    UUID parkingId,
    UUID floorId,
    String code,
    String name,
    String vehicleTypeCode,
    String vehicleTypeName,
    int capacity,
    ZoneStatus status) {}
