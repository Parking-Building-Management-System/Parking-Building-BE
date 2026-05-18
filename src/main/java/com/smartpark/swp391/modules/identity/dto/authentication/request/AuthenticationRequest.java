package com.smartpark.swp391.modules.identity.dto.authentication.request;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String deviceFingerprint,
    String deviceLabel) {}
