package com.smartpark.swp391.modules.admin.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.admin.dto.security.AdminDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.AdminSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.AuditLogResponse;
import com.smartpark.swp391.modules.admin.dto.security.ForceLogoutResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.SecurityActionRequest;
import java.time.Instant;
import java.util.UUID;

public interface AdminAuditSecurityService {
  PageResponse<AuditLogResponse> getAuditLogs(
      UUID actorId, String role, String severity, Instant from, Instant to, int page, int size);

  PageResponse<AdminSessionResponse> getSessions(
      UUID tenantId, String role, String status, int page, int size);

  ForceLogoutResponse forceLogout(UUID userId, SecurityActionRequest request);

  RevokeSessionResponse revokeSession(UUID sessionId, SecurityActionRequest request);

  PageResponse<AdminDeviceResponse> getDevices(UUID tenantId, String status, int page, int size);

  RevokeDeviceResponse revokeDevice(UUID deviceId, SecurityActionRequest request);
}
