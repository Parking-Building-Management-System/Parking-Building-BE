package com.smartpark.swp391.modules.manager.dto.device;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceApprovalRequest(@NotNull UUID kioskId, LocalDateTime expiresAt) {}
