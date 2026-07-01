package com.smartpark.swp391.modules.settlement.dto;

import lombok.Builder;

@Builder
public record StaffCurrentCashShiftResponse(boolean hasOpenShift, StaffCashShiftResponse shift) {}
