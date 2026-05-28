# System Admin Audit & Security API Spec

## Scope

These APIs let `SYSTEM_ADMIN` inspect audit logs, review sessions/devices across tenants, and revoke risky sessions or devices.

## Endpoints

### GET /admin/audit/logs?actorId=&role=&severity=&from=&to=&page=&size=

Purpose: search audit events across all tenants and global admin actions.

Audit log item:

```json
{
  "id": "uuid",
  "tenantId": "uuid-or-null",
  "actorId": "uuid",
  "actorUsername": "admin@smartpark.local",
  "actorRole": "SYSTEM_ADMIN",
  "action": "TENANT_STATUS_UPDATED",
  "resourceType": "TENANT",
  "resourceId": "uuid",
  "severity": "INFO",
  "reason": "Routine tenant administration",
  "ipAddress": "10.20.1.42",
  "deviceFingerprint": "fingerprint",
  "createdAt": "2026-05-28T03:18:00Z"
}
```

### GET /admin/sessions?tenantId=&role=&status=&page=&size=

Purpose: list sessions across tenants.

Session item:

```json
{
  "id": "uuid",
  "userId": "uuid",
  "username": "manager@tenant.local",
  "role": "PARKING_MANAGER",
  "tenantId": "uuid",
  "tenantName": "BCONS Plaza",
  "deviceId": "uuid",
  "deviceLabel": "Office laptop",
  "status": "ACTIVE",
  "createdAt": "2026-05-28T01:12:00Z",
  "expiresAt": "2026-05-29T01:12:00Z",
  "revokedAt": null
}
```

### POST /admin/users/{userId}/force-logout

Purpose: revoke all current active sessions for one user.

Request:

```json
{
  "reason": "Account compromise investigation"
}
```

Response:

```json
{
  "userId": "uuid",
  "revokedSessionCount": 3
}
```

### POST /admin/sessions/{sessionId}/revoke

Purpose: revoke a single active session.

Request:

```json
{
  "reason": "Suspicious session"
}
```

### GET /admin/devices?tenantId=&status=&page=&size=

Purpose: list devices across tenants.

Device item:

```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "tenantName": "BCONS Plaza",
  "userId": "uuid",
  "username": "staff-entry@tenant.local",
  "label": "Kiosk tablet B1",
  "fingerprint": "fingerprint",
  "status": "APPROVED",
  "createdAt": "2026-05-28T01:00:00Z",
  "approvedAt": "2026-05-28T01:10:00Z",
  "revokedAt": null
}
```

### POST /admin/devices/{deviceId}/revoke

Purpose: revoke a trusted device without deleting its row.

Request:

```json
{
  "reason": "Lost device"
}
```

## Field Reference

Audit log fields:

- `id`
- `tenantId` nullable for global actions
- `actorId`
- `actorUsername`
- `actorRole`
- `action`
- `resourceType`
- `resourceId`
- `severity`
- `reason`
- `ipAddress`
- `deviceFingerprint`
- `createdAt`

Session fields:

- `id`
- `userId`
- `username`
- `role`
- `tenantId`
- `tenantName`
- `deviceId`
- `deviceLabel`
- `status`
- `createdAt`
- `expiresAt`
- `revokedAt`

## Security Rules

- `SYSTEM_ADMIN` only.
- System Admin can inspect across tenants.
- Force logout must revoke current active sessions.
- Device revoke must not delete device rows; mark the device revoked, suspended, or inactive according to backend status conventions.
- Every force logout and device revoke action must create an audit log entry.
- API responses must not expose raw refresh tokens, access tokens, password hashes, or secrets.

