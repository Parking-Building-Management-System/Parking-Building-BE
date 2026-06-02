package com.smartpark.swp391.modules.staff.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffFireInspectionDueResponse(
    UUID fireExtinguisherId,
    String code,
    FireExtinguisherType type,
    FireExtinguisherStatus status,
    String parkingName,
    String floorName,
    String zoneName,
    String locationDescription,
    LocalDate expiryDate,
    LocalDateTime lastInspectedAt,
    LocalDateTime nextInspectionAt) {}
