package com.smartpark.swp391.modules.manager.dto.facility;

import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ParkingStatusResponse(UUID id, ParkingStatus status) {}
