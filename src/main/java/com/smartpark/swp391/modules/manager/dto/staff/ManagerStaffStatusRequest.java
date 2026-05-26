package com.smartpark.swp391.modules.manager.dto.staff;

import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerStaffStatusRequest(@NotNull UserStatus status) {}
