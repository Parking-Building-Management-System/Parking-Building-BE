package com.smartpark.swp391.modules.staff.dto.lostcard;

import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffLostCardCompleteExitResponse(
    UUID sessionId,
    ParkingSessionStatus status,
    String plateNumber,
    LocalDateTime checkOutAt,
    BigDecimal parkingAmountDue,
    BigDecimal surchargeAmountDue,
    BigDecimal penaltyAmountDue,
    BigDecimal totalAmountDue,
    BigDecimal collectedAmount,
    String currency,
    String slotCode,
    SlotStatus slotStatus,
    String cardCode,
    RfidCardStatus cardStatus,
    String message) {}
