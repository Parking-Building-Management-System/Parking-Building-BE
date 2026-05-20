package com.smartpark.swp391.modules.manager.dto.facility;

import java.util.UUID;
import lombok.Builder;

@Builder
public record FloorResponse(
    UUID id, UUID parkingId, String code, String name, int displayOrder, boolean active) {}
