package com.smartpark.swp391.modules.settlement.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record StaffCashSettlementPreviewResponse(
    StaffCashShiftResponse shift,
    BigDecimal expectedCashAmount,
    BigDecimal onlineAmount,
    BigDecimal cashParkingAmount,
    BigDecimal surchargeCashAmount,
    BigDecimal penaltyCashAmount,
    BigDecimal lostCardCashAmount,
    Integer transactionCount,
    List<StaffCashTransactionResponse> recentTransactions) {}
