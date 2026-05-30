package com.smartpark.swp391.modules.vehicle.dto;

import java.time.LocalDateTime;

public record VehicleTypeDTO(
    String id, String name, String description, LocalDateTime createdDate, LocalDateTime updatedDate) {}
