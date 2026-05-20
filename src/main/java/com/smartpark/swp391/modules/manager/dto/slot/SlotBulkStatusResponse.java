package com.smartpark.swp391.modules.manager.dto.slot;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import lombok.Builder;

@Builder
public record SlotBulkStatusResponse(int updatedCount, SlotStatus newStatus) {}
