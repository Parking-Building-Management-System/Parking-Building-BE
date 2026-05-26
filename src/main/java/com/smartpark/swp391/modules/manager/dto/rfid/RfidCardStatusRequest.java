package com.smartpark.swp391.modules.manager.dto.rfid;

import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import jakarta.validation.constraints.NotNull;

public record RfidCardStatusRequest(@NotNull RfidCardStatus status) {}
