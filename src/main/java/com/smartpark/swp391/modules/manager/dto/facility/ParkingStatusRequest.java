package com.smartpark.swp391.modules.manager.dto.facility;

import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import jakarta.validation.constraints.NotNull;

public record ParkingStatusRequest(@NotNull ParkingStatus status) {}
