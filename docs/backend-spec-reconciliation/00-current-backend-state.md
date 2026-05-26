# 00. Current Backend State

Audit date: 2026-05-26. Scope: code under `src/main/java`, Flyway migrations under `src/main/resources/db/migration`, config under `src/main/resources/application.yaml`, and existing `docs/`.

## Backend Modules Found

- `identity`: auth, JWT, users, roles, permissions, tenant, devices, sessions.
- `admin`: system admin dashboard, tenant provisioning, tenant status toggle, global vehicle type CRUD, role list.
- `manager`: facility and slot APIs for `PARKING_MANAGER`.
- `parking`: entities/repositories for parking, floor, zone, slot, RFID card.
- `operation`: entities for parking session, shift, kiosk, kiosk staff, zone violation report. No operation controller/service found.
- `billing`: entities for invoice and webhook log. No billing controller/service/repository found.
- `subscription`: entity and enum only. No controller/service/repository found.
- `audit`: `AuditLog` entity only. No audit controller/service/repository found.
- `notification`: `Notification` entity only. No controller/service/repository found.
- `dashboard`: API traffic log entity/repository used by admin dashboard.
- `infrastructure.cached.redis`: Redis key and cache services.
- `infrastructure.tenant`: `TenantContext` and repository aspect enabling/disabling Hibernate tenant filter.

## Current Auth / Session / Device State

Auth login is internal username/password. `AuthenticationServiceImpl` validates username, password, user status, tenant status, and device fingerprint before creating `Session`. JWT access/refresh tokens include `user_id`, `tenant_id`, and `session_id`. Refresh token rotation is backed by `sessions.refresh_jti`. Session authorization is loaded through `SessionAuthorityResolver`, cached in Redis, and guarded by DB fallback.

Device binding is PARTIAL: unknown device creates a `Device` with `PENDING` and login is blocked, and approved devices can login. There is no manager API found for pending device list, approve/reject, temporary 8-hour approval, bind new kiosk/device, or revoke device.

Kill switch is PARTIAL/DONE depending scope: `/auth/logout`, `/auth/logout-all`, `/auth/admin/users/{userId}/force-logout`, and admin tenant suspension revoke active sessions. There is no manager-facing force logout or inactive user API found.

## Current Tenant State

Tenant entity and migration exist. JWT contains `tenant_id`. Core business tables mostly have direct `tenant_id`. Hibernate filter foundation exists through `TenantScopedEntity`, `TenantFilterAspect`, and `TenantContext`. Manager facility controllers call `ManagerTenantContext` to set tenant from JWT.

Important risk: tenant filter is not globally bound for every authenticated request. Only manager controllers using `ManagerTenantContext` are explicitly scoped. Repositories disable tenant filter when `TenantContext` is empty. Admin endpoints bypass tenant filter by not setting context, which is useful for global admin, but any future manager/staff/user endpoint that forgets to set `TenantContext` may query globally.

## Current API State

- Auth APIs: `/auth/login`, `/auth/refresh`, `/auth/me`, `/auth/logout`, `/auth/logout-all`, `/auth/admin/users/{userId}/force-logout`.
- Legacy tenant APIs: `/tenants` create/get/suspend/delete exists in `TenantController`; docs note earlier versions lacked role enforcement, but current audit should treat `/admin/tenants` as the main System Admin contract.
- Admin APIs: `/admin/dashboard/stats`, `/admin/tenants`, `/admin/tenants/{id}/status`, `/admin/master-data/vehicle-types`, `/admin/master-data/roles`.
- Manager facility APIs: `/manager/parkings`, `/manager/parkings/{id}/status`, `/manager/parkings/{id}/topology`, floor CRUD, zone CRUD.
- Manager slot APIs: search, bulk status, Excel import, Excel export.
- Staff/User/PWA operation APIs: MISSING.

## Entity / Migration State

Entities with migrations found: `Tenant`, `User`, `Role`, `Permission`, `UserRole`, `RolePermission`, `Device`, `Session`, `VehicleType`, `Parking`, `Floor`, `Zone`, `Slot`, `RfidCard`, `UserVehicleLink`, `Shift`, `Kiosk`, `KioskStaff`, `ParkingSession`, `Subscription`, `Invoice`, `WebhookLog`, `AuditLog`, `ZoneViolationReport`, `Notification`, `ApiTrafficLog`.

No `ENTITY_WITHOUT_MIGRATION` was found in this pass for Java entities under `src/main/java/com/smartpark/swp391/modules/**/entity/*.java`.

Naming caveat: Java entity `Invoice` maps to table `invoice`, while the business spec uses `invoices`. Current backend has `invoice`, not `invoices`.

## Redis / Cache State

Redis is configured through `RedisConfig`, `StringRedisTemplate`, and services:

- session authz cache: `SessionAuthorityCacheService`
- tenant detail cache: `TenantCacheService`
- admin dashboard and vehicle type cache: `AdminPortalCacheService`
- manager parking topology cache: `ManagerFacilityCacheService`
- rate limit token bucket: `RateLimitService`

## Swagger / OpenAPI State

Springdoc is configured in `SwaggerConfig`. Controllers are annotated with OpenAPI metadata. Existing docs reference Swagger UI at `/swagger-ui/index.html` and API docs at `/v3/api-docs`.

## Docs-Only Or Stale Docs Observed

Some docs such as `docs/backend-base-architecture-evaluation.md` and parts of `docs/00-backend-overview.md` describe missing parking/slot/invoice modules. Those statements are stale relative to current code because migrations/entities/controller/service now exist for facility/admin parts. Treat current source code and migrations as authority.

| Area | Status | Evidence Files | Notes |
|---|---|---|---|
| Auth Login | DONE | `src/main/java/com/smartpark/swp391/modules/identity/controller/AuthenticationController.java`, `src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/AuthenticationServiceImpl.java` | Username/password, tenant status, user status, approved device check. |
| JWT Refresh | DONE | `AuthenticationController.java`, `AuthenticationServiceImpl.java`, `src/main/java/com/smartpark/swp391/modules/identity/repository/SessionRepository.java`, `src/main/java/com/smartpark/swp391/modules/identity/service/token/impl/TokenServiceImpl.java` | Refresh token requires `typ=REFRESH`, rotates `refresh_jti`. |
| Session Store | DONE | `src/main/java/com/smartpark/swp391/modules/identity/entity/Session.java`, `V20260516162100__init_identity_tables.sql`, `SessionGuardService.java` | DB sessions plus Redis markers/cache. |
| Redis | DONE | `src/main/java/com/smartpark/swp391/infrastructure/cached/redis/**`, `src/main/resources/application.yaml` | Used for session authz, revoked/active markers, tenant/detail, admin cache, topology cache, rate limit. |
| Device Binding | PARTIAL | `Device.java`, `DeviceRepository.java`, `AuthenticationServiceImpl.java`, `V20260516162100__init_identity_tables.sql` | Unknown device creates PENDING and blocks login; approval/revoke/temporary APIs are MISSING. |
| Kill Switch | PARTIAL | `AuthenticationController.java`, `SessionRepository.java`, `AdminTenantManagementServiceImpl.java` | Admin force logout and tenant suspension revoke exist; manager kill switch/inactive staff APIs are MISSING. |
| Tenant CRUD | PARTIAL | `TenantController.java`, `AdminTenantManagementController.java`, `AdminTenantManagementServiceImpl.java`, `Tenant.java` | Admin list/provision/toggle exists. Legacy `/tenants` exists. No full update/delete under `/admin/tenants` found. |
| Vehicle Type CRUD | DONE | `AdminMasterDataController.java`, `AdminMasterDataServiceImpl.java`, `VehicleType.java`, `VehicleTypeRepository.java`, `V20260520100000__init_core_domain_tables.sql` | Global CRUD/list/delete soft-delete. |
| Parking CRUD | PARTIAL | `ManagerFacilityController.java`, `ManagerFacilityServiceImpl.java`, `Parking.java`, `ParkingRepository.java` | Manager list/toggle/topology only. Create/update/delete parking API MISSING. |
| Floor CRUD | DONE | `ManagerFacilityController.java`, `ManagerFacilityServiceImpl.java`, `Floor.java`, `FloorRepository.java`, `V20260520130000__add_manager_facility_metadata.sql` | Tenant-scoped through `ManagerTenantContext` + Hibernate filter. |
| Zone CRUD | DONE | `ManagerFacilityController.java`, `ManagerFacilityServiceImpl.java`, `Zone.java`, `ZoneRepository.java` | Tenant-scoped through `ManagerTenantContext` + Hibernate filter. |
| Slot CRUD | PARTIAL | `ManagerSlotController.java`, `ManagerSlotServiceImpl.java`, `Slot.java`, `SlotRepository.java` | Search, bulk status, import/export exist. Single create/update/delete API MISSING. |
| Multi-tenant Filter | PARTIAL | `TenantScopedEntity.java`, `TenantFilterAspect.java`, `TenantContext.java`, `ManagerTenantContext.java` | Hibernate filter exists but request binding is not global; future non-manager endpoints can bypass if context not set. |
| Pricing | MISSING | `rg Pricing` found no pricing entity/controller/service | Permissions mention pricing in seed, but no code/migration table for pricing policy/matrix. |
| Subscription | PARTIAL | `Subscription.java`, `V20260520100000__init_core_domain_tables.sql` | Entity/migration/seed only; no repository/service/controller/job. |
| Parking Session | PARTIAL | `ParkingSession.java`, `V20260520100000__init_core_domain_tables.sql` | Entity/migration/seed only; no entry/exit service/controller. |
| Invoice/Payment | PARTIAL | `Invoice.java`, `WebhookLog.java`, `V20260520100000__init_core_domain_tables.sql` | Invoice/webhook entity+migration only. Payment entity/API MISSING. |
| Staff Shift/Blind Drop | PARTIAL | `Shift.java`, `KioskStaff.java`, `V20260520100000__init_core_domain_tables.sql` | Shift schedule and kiosk assignment only. StaffShift actual shift session/cash drop/blind drop APIs MISSING. |
| Incident/Red Flag Audit | PARTIAL | `AuditLog.java`, `ZoneViolationReport.java`, `V20260520100000__init_core_domain_tables.sql` | Generic audit and zone violation entities exist. RedFlagAction/Incident API, required reason/evidence workflows MISSING. |
