package com.smartpark.swp391.modules.pwa.service;

import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;

public interface PwaCardSessionService {
  CardActiveSessionResponse getActiveSession(String qrToken);
}
