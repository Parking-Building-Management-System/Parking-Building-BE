package com.smartpark.swp391.modules.admin.dto.masterdata;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminVehicleTypeResponse(
    UUID id, String name, String code, boolean active, LocalDateTime createdAt) {}
