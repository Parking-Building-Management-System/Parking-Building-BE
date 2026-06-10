package com.smartpark.swp391.infrastructure.demo;

import lombok.Builder;

@Builder
public record FacilityMapDemoSeedResult(
    boolean demoSeedEnabled,
    boolean floorMapsEnabled,
    boolean slotCoordinatesEnabled,
    boolean storageConfigured,
    String bucketName,
    int assetCountFound,
    int nonDeletedFloorCount,
    int floorsWithMapImageUrlCount,
    int floorMapsSeeded,
    int floorMapsUploaded,
    int floorMapsReuploaded,
    int floorMapsSkippedConfigured,
    int floorMapsSkippedUnconfigured,
    int slotCoordinatesSeeded,
    int slotCoordinatesSkippedConfigured,
    boolean floorMapsSkippedMissingStorage) {}
