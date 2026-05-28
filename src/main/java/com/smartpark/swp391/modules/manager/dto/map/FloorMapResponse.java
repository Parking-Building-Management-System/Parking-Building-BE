package com.smartpark.swp391.modules.manager.dto.map;

import java.util.UUID;
import lombok.Builder;

@Builder
public record FloorMapResponse(
    UUID floorId, String floorName, UUID parkingId, String parkingName, String mapImageUrl) {}
