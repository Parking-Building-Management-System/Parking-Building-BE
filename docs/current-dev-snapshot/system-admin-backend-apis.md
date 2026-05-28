# System Admin Backend APIs

## Implemented Endpoints

Permissions and roles:

- `GET /admin/permissions/tree`
- `GET /admin/roles`
- `GET /admin/roles/{roleId}/permissions`
- `PUT /admin/roles/{roleId}/permissions`
- `POST /admin/permissions`
- `PUT /admin/permissions/{id}`
- `DELETE /admin/permissions/{id}`

System health:

- `GET /admin/system-health/summary`
- `GET /admin/system-health/services`
- `GET /admin/system-health/traffic?from=&to=&granularity=`
- `GET /admin/system-health/top-endpoints?from=&to=&limit=`
- `GET /admin/system-health/errors?from=&to=`

Audit and security:

- `GET /admin/audit/logs?actorId=&role=&severity=&from=&to=&page=&size=`
- `GET /admin/sessions?tenantId=&role=&status=&page=&size=`
- `POST /admin/users/{userId}/force-logout`
- `POST /admin/sessions/{sessionId}/revoke`
- `GET /admin/devices?tenantId=&status=&page=&size=`
- `POST /admin/devices/{deviceId}/revoke`

All endpoints are restricted to `SYSTEM_ADMIN`.

## Permission Tree

Permissions remain global master data. The backend queries active, non-deleted permission rows and builds the tree in one pass using ordered maps:

```text
scope -> module -> resource -> label -> actions
```

Sample:

```json
[
  {
    "scope": "PARKING_MANAGER",
    "modules": [
      {
        "module": "FACILITY",
        "resources": [
          {
            "resource": "SLOT",
            "labels": [
              {
                "label": "Slot Management",
                "actions": [
                  {
                    "id": "uuid",
                    "action": "VIEW",
                    "selected": true
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
]
```

`GET /admin/permissions/tree` omits `selected`. `GET /admin/roles/{roleId}/permissions` includes `selected`.

Permission tree cache is stored in Redis when available. The all-permission tree is evicted on permission create/update/delete. Per-role permission tree cache is evicted on role assignment replacement.

## DB Tables

Existing tables used:

- `roles`
- `permissions`
- `role_permissions`
- `user_roles`
- `audit_logs`
- `sessions`
- `devices`
- `api_traffic_logs`
- `tenants`
- `users`

Migration added:

- `V20260528170000__system_admin_security_api_fields.sql`

It adds:

- `permissions.description`
- `permissions.status`
- `permissions.is_deleted`
- `audit_logs.actor_role`
- `audit_logs.severity`
- `audit_logs.reason`
- `audit_logs.device_fingerprint`
- search indexes for permissions, audit logs, and sessions

## Health APIs

`summary` uses current JVM uptime, active tenant/session counts, and the last 24 hours of `api_traffic_logs`.

`services` checks:

- backend app
- database connection
- Redis ping
- MinIO/S3 bucket presence when storage is configured

Storage health reads `APP_MINIO_*` mapped through `app.storage.minio.*`. It reports `UNCONFIGURED` when MinIO is not configured and never returns credentials.

Traffic, top endpoints, and errors are based on `api_traffic_logs`. If telemetry is sparse, responses are real but may be empty.

## Audit And Security APIs

Audit logs return stable fields from `audit_logs`, with nullable fields where older rows do not have metadata.

Session listing uses `sessions`, `users`, `tenants`, `devices`, and `user_roles`. Status is derived:

- `ACTIVE`: not revoked and not expired
- `REVOKED`: `revoked_at` is set
- `EXPIRED`: not revoked and expired

Force logout and session revoke update `sessions.revoked_at` and clear Redis session auth/active markers best-effort. Device revoke marks `devices.status = SUSPENDED` and revokes active sessions for that device user.

Device fingerprints are shortened in API responses.

## Request Samples

Replace role permissions:

```json
{
  "permissionIds": ["uuid", "uuid"]
}
```

Create permission:

```json
{
  "scope": "PARKING_MANAGER",
  "module": "FACILITY",
  "resource": "SLOT",
  "label": "Slot Management",
  "action": "VIEW",
  "description": "Can view slots"
}
```

Force logout:

```json
{
  "reason": "Account compromise investigation"
}
```

## FE Pages Ready For Real APIs

- `/admin/master-data/roles-permissions`
- `/admin/system-health/api`
- `/admin/system-health/traffic`
- `/admin/audit/logs`
- `/admin/audit/sessions`

## Known Pending Items

- No request logging filter was added in this slice. Health traffic uses existing `api_traffic_logs`.
- Older audit seed rows do not contain actor role, severity reason, or device fingerprint, so those fields can be `null` or defaulted.
- Device revoke currently revokes active sessions for the device owner, matching existing user-session revocation semantics.
