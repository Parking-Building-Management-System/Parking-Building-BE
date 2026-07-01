package com.smartpark.swp391.modules.settlement.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementDetailResponse;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementListItemResponse;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ManagerShiftSettlementService {

  PageResponse<ManagerShiftSettlementListItemResponse> getSettlements(
      UUID parkingId,
      UUID staffId,
      StaffCashShiftStatus status,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size);

  ManagerShiftSettlementDetailResponse getSettlement(UUID id);
}
