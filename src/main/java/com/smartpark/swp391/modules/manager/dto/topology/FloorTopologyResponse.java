package com.smartpark.swp391.modules.manager.dto.topology;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FloorTopologyResponse(
    UUID id, String code, String name, int displayOrder, List<ZoneTopologyResponse> zones) {}
