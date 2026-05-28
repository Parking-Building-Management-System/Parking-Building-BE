package com.smartpark.swp391.modules.manager.dto.map;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FloorMapDetailResponse(
    UUID floorId,
    String floorName,
    UUID parkingId,
    String parkingName,
    String mapImageUrl,
    String coordinateMode,
    List<SlotMapItemResponse> slots) {}
