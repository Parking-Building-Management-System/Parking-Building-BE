package com.smartpark.swp391.modules.identity.service.auth;

import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import java.util.UUID;

public interface AuthorityLoader {
  SessionAuthzCache load(UUID userId);
}
