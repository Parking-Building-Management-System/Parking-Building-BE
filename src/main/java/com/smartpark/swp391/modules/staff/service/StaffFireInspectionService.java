package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionDueResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignResponse;
import java.util.List;
import java.util.UUID;

public interface StaffFireInspectionService {

  List<StaffFireInspectionDueResponse> getDueInspections(
      UUID floorId, FireExtinguisherStatus status);

  StaffInspectionPhotoPresignResponse createPhotoUpload(StaffInspectionPhotoPresignRequest request);

  StaffFireInspectionResponse submitInspection(StaffFireInspectionRequest request);
}
