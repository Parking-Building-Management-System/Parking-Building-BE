package com.smartpark.swp391.modules.manager.dto.firesafety;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FireSafetyMapResponse(
    UUID parkingId,
    String parkingName,
    UUID floorId,
    String floorName,
    String floorCode,
    String mapImageUrl,
    String coordinateMode,
    List<FireSafetyMapItemResponse> extinguishers) {}
