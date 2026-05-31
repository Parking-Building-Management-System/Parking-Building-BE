package com.smartpark.swp391.modules.staff.dto.exit;

import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CompleteExitResponse(
    UUID sessionId,
    ParkingSessionStatus status,
    String plateNumber,
    String cardCode,
    LocalDateTime checkInAt,
    LocalDateTime checkOutAt,
    ExitPaymentMode paymentMode,
    BigDecimal collectedAmount,
    BigDecimal totalAmount,
    String currency,
    String slotCode,
    SlotStatus slotStatus,
    RfidCardStatus cardStatus,
    String message) {}
