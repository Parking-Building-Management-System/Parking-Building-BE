package com.smartpark.swp391.modules.admin.dto.security;

import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RevokeDeviceResponse(UUID deviceId, DeviceStatus status, int revokedSessionCount) {}
