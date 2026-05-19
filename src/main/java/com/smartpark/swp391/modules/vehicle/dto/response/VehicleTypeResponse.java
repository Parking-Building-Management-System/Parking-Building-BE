package com.smartpark.swp391.modules.vehicle.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record VehicleTypeResponse(
    UUID id, String name, String code, boolean active, LocalDateTime createdAt) {}
