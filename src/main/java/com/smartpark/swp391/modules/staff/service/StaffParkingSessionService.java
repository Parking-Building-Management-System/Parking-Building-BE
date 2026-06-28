package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewRequest;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewResponse;

public interface StaffParkingSessionService {

  ParkingSessionCheckInResponse checkIn(ParkingSessionCheckInRequest request);

  ParkingSessionPhotoPresignResponse createPhotoUpload(ParkingSessionPhotoPresignRequest request);

  ExitPreviewResponse previewExit(ExitPreviewRequest request);

  CompleteExitResponse completeExit(CompleteExitRequest request);
}
