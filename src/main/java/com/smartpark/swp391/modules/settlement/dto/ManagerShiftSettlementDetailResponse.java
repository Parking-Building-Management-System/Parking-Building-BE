package com.smartpark.swp391.modules.settlement.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ManagerShiftSettlementDetailResponse(
    StaffCashShiftResponse shift, List<StaffCashTransactionResponse> transactions) {}
