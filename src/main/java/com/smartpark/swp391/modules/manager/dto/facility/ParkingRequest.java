package com.smartpark.swp391.modules.manager.dto.facility;

import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParkingRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 255) String name,
    String address,
    ParkingStatus status) {}
