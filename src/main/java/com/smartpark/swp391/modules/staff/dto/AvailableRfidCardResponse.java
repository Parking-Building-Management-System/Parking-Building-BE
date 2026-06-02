package com.smartpark.swp391.modules.staff.dto;

import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AvailableRfidCardResponse(
    UUID id, String code, String label, RfidCardStatus status) {}
