package com.smartpark.swp391.modules.pwa.service;

import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.dto.CardCheckoutQuoteResponse;

public interface PwaCardSessionService {
  CardActiveSessionResponse getActiveSession(String qrToken);

  CardCheckoutQuoteResponse getCheckoutQuote(String qrToken);
}
