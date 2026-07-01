package com.smartpark.swp391.modules.staff.dto;

import com.smartpark.swp391.modules.operation.enumType.KioskType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffResolvedContext(
    UUID tenantId,
    UUID staffId,
    UUID kioskId,
    String kioskName,
    KioskType kioskType,
    UUID parkingId,
    String parkingName) {}
