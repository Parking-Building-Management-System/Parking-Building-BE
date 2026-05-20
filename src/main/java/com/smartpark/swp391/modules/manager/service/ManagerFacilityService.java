package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.manager.dto.facility.FloorRequest;
import com.smartpark.swp391.modules.manager.dto.facility.FloorResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingStatusResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneRequest;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneResponse;
import com.smartpark.swp391.modules.manager.dto.topology.ParkingTopologyResponse;
import java.util.List;
import java.util.UUID;

public interface ManagerFacilityService {
  List<ParkingResponse> getParkings();

  ParkingStatusResponse toggleParkingStatus(UUID id);

  ParkingTopologyResponse getTopology(UUID parkingId);

  List<FloorResponse> getFloors(UUID parkingId);

  FloorResponse getFloor(UUID id);

  FloorResponse createFloor(UUID parkingId, FloorRequest request);

  FloorResponse updateFloor(UUID id, FloorRequest request);

  void deleteFloor(UUID id);

  List<ZoneResponse> getZones(UUID floorId);

  ZoneResponse getZone(UUID id);

  ZoneResponse createZone(UUID floorId, ZoneRequest request);

  ZoneResponse updateZone(UUID id, ZoneRequest request);

  void deleteZone(UUID id);
}
