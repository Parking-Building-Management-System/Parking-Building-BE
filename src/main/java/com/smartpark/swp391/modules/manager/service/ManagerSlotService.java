package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotImportResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ManagerSlotService {
  PageResponse<SlotResponse> getSlots(
      UUID zoneId, SlotStatus status, String slotCode, boolean exact, int page, int size);

  SlotBulkStatusResponse bulkUpdateStatus(SlotBulkStatusRequest request);

  SlotImportResponse importSlots(MultipartFile file);

  byte[] exportSlots();
}
