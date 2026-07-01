package com.smartpark.swp391.modules.settlement.service;

import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionSource;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import java.math.BigDecimal;

public record StaffCashLedgerEntry(
    StaffCashTransactionType type,
    BigDecimal amount,
    ParkingSession parkingSession,
    PenaltyCase penaltyCase,
    StaffCashTransactionSource source,
    String note) {}
