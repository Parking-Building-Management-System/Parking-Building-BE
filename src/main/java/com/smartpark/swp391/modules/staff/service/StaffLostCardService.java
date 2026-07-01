package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPreviewResponse;

public interface StaffLostCardService {

  StaffLostCardPhotoPresignResponse createPhotoUpload(StaffLostCardPhotoPresignRequest request);

  StaffLostCardPreviewResponse previewByPlate(String plateNumber);

  StaffLostCardCaseResponse createCase(StaffLostCardCaseRequest request);

  StaffLostCardCompleteExitResponse completeExit(StaffLostCardCompleteExitRequest request);
}
