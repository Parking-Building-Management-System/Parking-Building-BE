package com.smartpark.swp391.modules.manager.dto.topology;

import com.smartpark.swp391.modules.parking.enumType.ZoneStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ZoneTopologyResponse(
    UUID id,
    String code,
    String name,
    String vehicleTypeCode,
    String vehicleTypeName,
    int capacity,
    long slotCount,
    ZoneStatus status) {}
