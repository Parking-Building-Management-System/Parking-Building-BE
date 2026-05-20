package com.smartpark.swp391.modules.manager.dto.slot;

import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SlotBulkStatusRequest(@NotEmpty List<UUID> slotIds, @NotNull SlotStatus newStatus) {}
