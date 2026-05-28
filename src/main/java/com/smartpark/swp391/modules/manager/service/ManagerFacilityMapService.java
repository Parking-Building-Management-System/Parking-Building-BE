package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.manager.dto.map.FloorMapDetailResponse;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapRequest;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateResponse;
import java.util.UUID;

public interface ManagerFacilityMapService {
  FloorMapResponse updateFloorMap(UUID floorId, FloorMapRequest request);

  FloorMapDetailResponse getFloorMap(UUID floorId);

  SlotCoordinateResponse updateSlotCoordinate(UUID slotId, SlotCoordinateRequest request);

  SlotCoordinateBulkResponse updateSlotCoordinates(SlotCoordinateBulkRequest request);
}
