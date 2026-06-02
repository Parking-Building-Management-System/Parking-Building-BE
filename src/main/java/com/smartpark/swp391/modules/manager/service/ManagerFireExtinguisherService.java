package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherStatusRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherSummaryResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireInspectionLogResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireSafetyMapResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ManagerFireExtinguisherService {

  PageResponse<FireExtinguisherResponse> getExtinguishers(
      UUID parkingId,
      UUID floorId,
      UUID zoneId,
      FireExtinguisherStatus status,
      FireExtinguisherType type,
      String search,
      Integer expiringWithinDays,
      int page,
      int size);

  FireExtinguisherResponse createExtinguisher(FireExtinguisherRequest request);

  FireExtinguisherResponse getExtinguisher(UUID id);

  FireExtinguisherResponse updateExtinguisher(UUID id, FireExtinguisherRequest request);

  FireExtinguisherResponse updateStatus(UUID id, FireExtinguisherStatusRequest request);

  FireExtinguisherResponse updateCoordinate(UUID id, FireExtinguisherCoordinateRequest request);

  void deleteExtinguisher(UUID id);

  FireExtinguisherSummaryResponse getSummary();

  FireSafetyMapResponse getFireSafetyMap(UUID floorId);

  PageResponse<FireInspectionLogResponse> getInspectionLogs(
      UUID extinguisherId,
      UUID parkingId,
      UUID floorId,
      FireInspectionResult result,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size);
}
