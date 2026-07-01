package com.smartpark.swp391.modules.settlement.service;

import com.smartpark.swp391.modules.settlement.dto.StaffCashSettlementPreviewResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftCloseRequest;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCurrentCashShiftResponse;

public interface StaffCashShiftService {

  StaffCashShiftResponse startShift();

  StaffCurrentCashShiftResponse getCurrentShift();

  StaffCashSettlementPreviewResponse getCurrentSettlementPreview();

  StaffCashShiftResponse closeCurrentShift(StaffCashShiftCloseRequest request);
}
