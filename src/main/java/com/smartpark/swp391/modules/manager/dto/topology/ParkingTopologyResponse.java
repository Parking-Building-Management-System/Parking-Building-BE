package com.smartpark.swp391.modules.manager.dto.topology;

import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ParkingTopologyResponse(
    UUID id,
    String code,
    String name,
    ParkingStatus status,
    int totalCapacity,
    List<FloorTopologyResponse> floors) {}
