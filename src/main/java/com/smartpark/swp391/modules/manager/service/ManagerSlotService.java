package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotImportResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotStatusRequest;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ManagerSlotService {
  PageResponse<SlotResponse> getSlots(
      UUID parkingId,
      UUID floorId,
      UUID zoneId,
      SlotStatus status,
      String slotCode,
      boolean exact,
      int page,
      int size);

  SlotResponse getSlot(UUID id);

  SlotResponse createSlot(UUID zoneId, SlotRequest request);

  SlotResponse updateSlot(UUID id, SlotRequest request);

  void deleteSlot(UUID id);

  SlotResponse updateSlotStatus(UUID id, SlotStatusRequest request);

  SlotBulkStatusResponse bulkUpdateStatus(SlotBulkStatusRequest request);

  SlotImportResponse importSlots(MultipartFile file);

  byte[] exportSlots();
}
