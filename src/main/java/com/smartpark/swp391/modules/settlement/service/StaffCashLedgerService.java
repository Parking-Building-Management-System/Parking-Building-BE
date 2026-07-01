package com.smartpark.swp391.modules.settlement.service;

import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import java.util.List;

public interface StaffCashLedgerService {

  void recordCashTransactions(StaffResolvedContext context, List<StaffCashLedgerEntry> entries);
}
