# Group 1 - Foundation, Identity & Administration APIs

## 1. Group Purpose

This group explains the foundation of SmartPark: authentication, JWT/session lifecycle, tenant onboarding, master data, permissions, audit, health, and platform security controls.

The key message for the teacher: SmartPark is multi-tenant and role-based. Every operational API depends on this foundation.

## 2. Main Actors

- `SYSTEM_ADMIN`: manages tenants, global master data, permissions, audit, sessions, devices, and health.
- `PARKING_MANAGER`: logs in through the same identity system, then manages one tenant.
- `STAFF`: logs in only after device trust and kiosk context are approved.

## 3. Endpoint Table

| Method | Path | Actor/Role | Purpose | Main Request DTO | Main Response DTO | DB tables touched |
|---|---|---|---|---|---|---|
| POST | `/auth/login` | Public credentials, then role checks | Authenticate user, validate device trust, create session, issue tokens | `AuthenticationRequest` | `AuthenticationResponse` | `users`, `tenants`, `roles`, `devices`, `sessions`, `kiosk`, `kiosk_staff` |
| POST | `/auth/refresh` | Authenticated by refresh token | Rotate refresh JTI and issue new token pair | Cookie/header refresh token | `AuthenticationResponse` | `sessions` |
| GET | `/auth/me` | Any authenticated role | Return profile, roles, permissions, staff work context if applicable | none | `UserProfileResponse` | `users`, `sessions`, `roles`, `permissions`, `kiosk`, `kiosk_staff` |
| POST | `/auth/logout` | Any authenticated role | Revoke current session and clear cookie | JWT principal | `ApiResponse<Void>` | `sessions`, Redis revoke marker |
| POST | `/auth/logout-all` | Any authenticated role | Revoke all own sessions | JWT principal | `ApiResponse<Void>` | `sessions`, Redis revoke markers |
| GET | `/admin/tenants` | `SYSTEM_ADMIN` | List tenant workspaces | page, size | `PageResponse<AdminTenantSummaryResponse>` | `tenants`, `users` |
| POST | `/admin/tenants` | `SYSTEM_ADMIN` | Provision tenant and first manager | `AdminTenantProvisionRequest` | `AdminTenantSummaryResponse` | `tenants`, `users`, `roles`, `user_roles`, `devices` |
| PATCH | `/admin/tenants/{id}/status` | `SYSTEM_ADMIN` | Toggle tenant active/suspended and revoke sessions | path id | `AdminTenantStatusResponse` | `tenants`, `sessions`, cache |
| GET | `/admin/master-data/vehicle-types` | `SYSTEM_ADMIN` | List global vehicle categories | none | `List<AdminVehicleTypeResponse>` | `vehicle_types`, Redis cache |
| POST | `/admin/master-data/vehicle-types` | `SYSTEM_ADMIN` | Create vehicle type | `AdminVehicleTypeRequest` | `AdminVehicleTypeResponse` | `vehicle_types` |
| PUT | `/admin/master-data/vehicle-types/{id}` | `SYSTEM_ADMIN` | Update vehicle type | `AdminVehicleTypeRequest` | `AdminVehicleTypeResponse` | `vehicle_types` |
| DELETE | `/admin/master-data/vehicle-types/{id}` | `SYSTEM_ADMIN` | Soft-delete vehicle type | path id | `ApiResponse<Void>` | `vehicle_types` |
| GET | `/admin/master-data/roles` | `SYSTEM_ADMIN` | List global roles | none | `List<AdminRoleResponse>` | `roles` |
| GET | `/admin/permissions/tree` | `SYSTEM_ADMIN` | Load permission tree for UI | none | `List<PermissionScopeNode>` | `permissions` |
| GET | `/admin/roles` | `SYSTEM_ADMIN` | List roles for permission editor | none | `List<AdminRoleResponse>` | `roles` |
| GET | `/admin/roles/{roleId}/permissions` | `SYSTEM_ADMIN` | Get role permission tree | path roleId | `List<PermissionScopeNode>` | `roles`, `permissions`, `role_permissions` |
| PUT | `/admin/roles/{roleId}/permissions` | `SYSTEM_ADMIN` | Replace permissions assigned to role | `RolePermissionUpdateRequest` | `RolePermissionUpdateResponse` | `roles`, `permissions`, `role_permissions`, auth cache |
| POST | `/admin/permissions` | `SYSTEM_ADMIN` | Create permission definition | `PermissionRequest` | `PermissionResponse` | `permissions` |
| PUT | `/admin/permissions/{id}` | `SYSTEM_ADMIN` | Update permission definition | `PermissionRequest` | `PermissionResponse` | `permissions` |
| DELETE | `/admin/permissions/{id}` | `SYSTEM_ADMIN` | Soft-delete permission if safe | path id | `ApiResponse<Void>` | `permissions`, `role_permissions` |
| GET | `/admin/system-health/summary` | `SYSTEM_ADMIN` | Dashboard health summary | none | `SystemHealthSummaryResponse` | `api_traffic_logs`, dependency checks |
| GET | `/admin/system-health/services` | `SYSTEM_ADMIN` | Check DB/Redis/MinIO service health | none | `List<ServiceHealthResponse>` | DB/Redis/MinIO checks |
| GET | `/admin/system-health/traffic` | `SYSTEM_ADMIN` | Timeline of API traffic | from, to, granularity | `List<TrafficPointResponse>` | `api_traffic_logs` |
| GET | `/admin/system-health/top-endpoints` | `SYSTEM_ADMIN` | Most used endpoints | from, to, limit | `List<TopEndpointResponse>` | `api_traffic_logs` |
| GET | `/admin/system-health/errors` | `SYSTEM_ADMIN` | Recent API errors | from, to | `List<SystemErrorResponse>` | `api_traffic_logs` |
| GET | `/admin/audit/logs` | `SYSTEM_ADMIN` | Search audit logs | filters, page, size | `PageResponse<AuditLogResponse>` | `audit_logs` |
| GET | `/admin/sessions` | `SYSTEM_ADMIN` | Search active/revoked sessions | filters, page, size | `PageResponse<AdminSessionResponse>` | `sessions`, `users`, `tenants`, `devices` |
| POST | `/admin/users/{userId}/force-logout` | `SYSTEM_ADMIN` | Revoke all sessions of target user | `SecurityActionRequest` | `ForceLogoutResponse` | `sessions`, `audit_logs`, cache |
| POST | `/admin/sessions/{sessionId}/revoke` | `SYSTEM_ADMIN` | Revoke one session | `SecurityActionRequest` | `RevokeSessionResponse` | `sessions`, `audit_logs`, cache |
| GET | `/admin/devices` | `SYSTEM_ADMIN` | Search trusted devices | filters, page, size | `PageResponse<AdminDeviceResponse>` | `devices`, `users`, `tenants` |
| POST | `/admin/devices/{deviceId}/revoke` | `SYSTEM_ADMIN` | Suspend device and revoke related trust | `SecurityActionRequest` | `RevokeDeviceResponse` | `devices`, `sessions`, `audit_logs`, cache |

## 4. Typical Demo Sequence

1. Call `POST /auth/login` as system admin.
2. Call `GET /auth/me` to show roles and permission claims.
3. Call `GET /admin/tenants`, then `POST /admin/tenants` if a fresh tenant is needed.
4. Call `GET /admin/master-data/vehicle-types`.
5. Call `GET /admin/permissions/tree` and `PUT /admin/roles/{roleId}/permissions`.
6. Call health endpoints to show monitoring.
7. Call audit/session/device endpoints to show security governance.

## 5. Important Validations and Business Rules

- Username/password must match an active user.
- Non-system-admin users require an active tenant.
- Staff login requires an approved device, active kiosk, and active staff-to-kiosk assignment.
- Manager first device can be auto-approved; later unknown manager devices become pending.
- Refresh token rotates `refresh_jti`; replay fails.
- Tenant suspension revokes tenant sessions and evicts tenant/cache data.
- Permission replacement verifies all permission IDs exist before rewriting `role_permissions`.
- Device/session revocation is non-destructive: rows remain for audit.

## 6. Common Error Codes

- `INVALID_INFO`: invalid username/password.
- `UNAUTHENTICATED`: missing/expired JWT or refresh token.
- `FORBIDDEN_ACTION`: wrong role, suspended tenant, suspended device, inactive user.
- `DEVICE_NOT_TRUST`: staff/manager device not approved.
- `RESOURCE_NOT_FOUND`: tenant, role, permission, session, device, or vehicle type not found.
- `DUPLICATE_RESOURCE`: duplicate tenant slug, vehicle type code, or permission definition.

## 7. How to Test With Bruno

Use these folders:

- `00 Auth`
- `01 System Admin`

Recommended variables:

- `baseUrl`
- `systemAdminToken`
- `tenantId`
- `roleId`
- `permissionId`
- `userId`
- `sessionRevokeId`
- `deviceId`

Run order:

1. `00 Auth/01 Login System Admin`.
2. Copy `accessToken` to `systemAdminToken`.
3. Run `00 Auth/04 Get Me`.
4. Run `01 System Admin/01 Get Tenants`.
5. Run permission, health, audit, sessions, and device requests as needed.

## 8. Presentation Notes

- Start by saying the platform is tenant-isolated and role-driven.
- Show that staff security is stricter than simple JWT: device trust plus kiosk context.
- Explain that admin APIs support monitoring and incident response, not only CRUD.
- Avoid showing real tokens. In Swagger/Bruno, collapse response fields or use demo tokens only.
