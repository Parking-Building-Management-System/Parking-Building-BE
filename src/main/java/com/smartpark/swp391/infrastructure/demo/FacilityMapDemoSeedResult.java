package com.smartpark.swp391.infrastructure.demo;

import lombok.Builder;

@Builder
public record FacilityMapDemoSeedResult(
    int floorMapsSeeded,
    int floorMapsSkippedConfigured,
    int slotCoordinatesSeeded,
    int slotCoordinatesSkippedConfigured,
    boolean floorMapsSkippedMissingStorage) {}
