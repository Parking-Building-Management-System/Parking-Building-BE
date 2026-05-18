package com.smartpark.swp391.modules.identity.dto.authentication.response;

import lombok.Builder;

@Builder
public record AuthenticationResponse(
    boolean authenticated, String accessToken, String refreshToken) {}
