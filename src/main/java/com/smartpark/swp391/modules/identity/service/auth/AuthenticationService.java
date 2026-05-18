package com.smartpark.swp391.modules.identity.service.auth;

import com.smartpark.swp391.modules.identity.dto.authentication.request.AuthenticationRequest;
import com.smartpark.swp391.modules.identity.dto.authentication.response.UserProfileResponse;
import com.smartpark.swp391.modules.identity.dto.token.response.TokenPair;
import java.util.UUID;

public interface AuthenticationService {
  TokenPair authenticate(AuthenticationRequest request);

  TokenPair refresh(String refreshToken);

  void logout(UUID sessionId, UUID userId);

  void logoutAll(UUID userId);

  void forceLogout(UUID targetUserId);

  UserProfileResponse getMyProfile();
}
