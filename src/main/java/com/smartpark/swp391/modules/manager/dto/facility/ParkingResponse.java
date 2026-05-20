package com.smartpark.swp391.modules.manager.dto.facility;

import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ParkingResponse(
    UUID id, String code, String name, String address, int totalCapacity, ParkingStatus status) {}
