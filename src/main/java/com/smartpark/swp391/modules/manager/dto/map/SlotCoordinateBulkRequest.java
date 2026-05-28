package com.smartpark.swp391.modules.manager.dto.map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SlotCoordinateBulkRequest(
    @NotEmpty @Size(max = 500) List<@Valid SlotCoordinateItemRequest> items) {}
