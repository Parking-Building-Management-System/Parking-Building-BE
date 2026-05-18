package com.smartpark.swp391.modules.identity.dto.authentication.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserProfileResponse(
    UUID id,
    UUID tenantId,
    String username,
    String fullName,
    String phone,
    List<String> roles,
    List<String> permissions) {}
