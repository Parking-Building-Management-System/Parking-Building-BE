package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateRequest;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardStatusRequest;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import java.util.UUID;

public interface ManagerRfidCardService {

  PageResponse<RfidCardResponse> getCards(RfidCardStatus status, int page, int size);

  RfidCardGenerateResponse generateCards(RfidCardGenerateRequest request);

  RfidCardResponse updateStatus(UUID id, RfidCardStatusRequest request);
}
