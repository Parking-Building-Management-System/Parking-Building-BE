package com.smartpark.swp391.modules.manager.dto.slot;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SlotResponse(
    UUID id,
    UUID parkingId,
    String parkingName,
    UUID floorId,
    String floorName,
    UUID zoneId,
    String zoneName,
    String code,
    String slotNumber,
    SlotStatus status) {}
