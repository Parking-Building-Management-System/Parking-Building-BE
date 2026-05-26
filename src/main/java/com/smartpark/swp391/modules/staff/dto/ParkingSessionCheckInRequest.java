package com.smartpark.swp391.modules.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ParkingSessionCheckInRequest(
    @NotBlank @Size(max = 30) String plateNumber,
    @NotBlank @Size(max = 100) String cardCode,
    @NotNull UUID parkingId,
    UUID vehicleTypeId,
    @Size(max = 1000) String entryImageUrl) {}
