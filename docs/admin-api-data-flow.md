# SYSTEM_ADMIN API Data Flow

This document explains the internal request lifecycle for the SYSTEM_ADMIN portal APIs:

- Dashboard stats
- Tenant management
- Master data: vehicle types and roles

Canonical frontend routes use `/admin/...`. Controllers do not expose a versioned prefix.

## 1. Security Boundary

All admin controllers are guarded with:

```java
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
```

Request flow:

1. Spring Security validates the bearer JWT.
2. `JwtAuthenticationConverter` resolves session authorization through Redis/DB.
3. The user must have `ROLE_SYSTEM_ADMIN`.
4. If the user is unauthenticated, the response is `401`.
5. If the user is authenticated but lacks `SYSTEM_ADMIN`, the response is `403`.

No `X-Tenant-Id` header is required for these endpoints. They are global SYSTEM_ADMIN operations.

## 2. Input Validation

Validation is handled at the controller boundary.

### Request Body Validation

Controller methods use `@Valid` on request DTOs:

```java
public ResponseEntity<ApiResponse<AdminTenantSummaryResponse>> provisionTenant(
    @Valid @RequestBody AdminTenantProvisionRequest request)
```

DTO constraints:

```java
public record AdminTenantProvisionRequest(
    @NotBlank @Size(max = 255) String companyName,
    @NotBlank @Email @Size(max = 255) String managerEmail,
    @NotBlank @Size(min = 8, max = 72) String initialPassword) {}
```

```java
public record AdminVehicleTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String code,
    Boolean active) {}
```

Validation failures are handled by `GlobalExceptionHandler.handleMethodArgumentNotValid()` and returned as:

```json
{
  "code": 4000,
  "message": "Validation failed",
  "result": null,
  "errors": {
    "fieldName": "validation message"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/..."
}
```

### Query Parameter Validation

`GET /admin/tenants` uses method-level validation:

```java
@RequestParam(defaultValue = "0") @Min(0) int page
@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
```

Invalid query parameters are handled as `ConstraintViolationException` and returned with code `4000`.

## 3. Caching Strategy: Redis Cache-Aside

The admin APIs use explicit cache-aside logic through `AdminPortalCacheService`.

### Dashboard Stats Cache

Redis key:

```text
smartpark:admin:dashboard:stats
```

TTL:

```text
10 minutes
```

Read flow:

1. `AdminDashboardServiceImpl.getStats()` calls `adminPortalCacheService.getDashboardStats()`.
2. On cache hit, the cached `AdminDashboardStatsResponse` is returned.
3. On cache miss:
   - Count active tenants from PostgreSQL.
   - Count parkings from PostgreSQL.
   - Aggregate API traffic telemetry from `api_traffic_logs`.
   - Save the whole response to Redis with a 10-minute TTL.
   - Return the loaded response.

Mutation eviction:

- `POST /admin/tenants` evicts `smartpark:admin:dashboard:stats`.
- `PATCH /admin/tenants/{id}/status` evicts `smartpark:admin:dashboard:stats`.

Vehicle type mutations do not evict dashboard stats because current dashboard stats do not depend on vehicle types.

### Vehicle Types Cache

Redis key:

```text
smartpark:admin:master-data:vehicle-types
```

TTL:

```text
No TTL. Cache remains until explicit mutation eviction.
```

Read flow:

1. `AdminMasterDataServiceImpl.getVehicleTypes()` calls `adminPortalCacheService.getVehicleTypes()`.
2. On cache hit, the cached list is returned.
3. On cache miss:
   - Load all non-deleted vehicle types from PostgreSQL ordered by name.
   - Save the list in Redis without TTL.
   - Return the loaded list.

Mutation eviction:

- `POST /admin/master-data/vehicle-types` evicts `smartpark:admin:master-data:vehicle-types`.
- `PUT /admin/master-data/vehicle-types/{id}` evicts `smartpark:admin:master-data:vehicle-types`.
- `DELETE /admin/master-data/vehicle-types/{id}` evicts `smartpark:admin:master-data:vehicle-types`.

### Tenant Cache Eviction

Existing tenant cache keys are also used:

```text
smartpark:tenant:detail:{tenantId}
smartpark:tenant:{tenantId}:*
```

When tenant status changes, `TenantCacheService.evictTenantData(id)` deletes:

- Tenant detail cache.
- Any tenant-scoped Redis keys matching the tenant pattern.

## 4. Database Interaction: PostgreSQL

### Dashboard Stats

Tables involved:

- `tenants`
- `parkings`
- `api_traffic_logs`

Queries:

- Active tenant count: `TenantRepository.countByStatusAndIsDeletedFalse(ACTIVE)`
- Parking count: `ParkingRepository.countByIsDeletedFalse()`
- Traffic aggregation: native query on `api_traffic_logs`, grouped by `date_trunc('day', occurred_at)`

Multi-tenant filter behavior:

- These are global SYSTEM_ADMIN queries.
- `tenants` is not tenant-scoped.
- `api_traffic_logs` is global telemetry and not tenant-scoped.
- `parkings` is tenant-scoped, but admin requests do not set `TenantContext`; the Hibernate tenant filter is not enabled, so global counts can see all tenants.

### Tenant Management

Tables involved:

- `tenants`
- `users`
- `roles`
- `user_roles`
- `sessions`

Provision flow:

1. Generate a slug from `companyName`.
2. Ensure slug uniqueness with suffixes like `company`, `company-2`.
3. Ensure `managerEmail` does not already exist as a username.
4. Insert a new `tenants` row with status `ACTIVE`.
5. Insert a new `users` row under that tenant.
6. BCrypt-hash `initialPassword`.
7. Load `PARKING_MANAGER` role.
8. Insert `user_roles` link for the manager.
9. Evict dashboard stats cache.

Status toggle flow:

1. Load tenant by ID.
2. Toggle:
   - `ACTIVE -> SUSPENDED`
   - `SUSPENDED -> ACTIVE`
3. Save tenant.
4. Evict tenant cache and admin dashboard stats cache.
5. If new status is `SUSPENDED`, revoke active sessions for all users under the tenant.

Multi-tenant filter behavior:

- Tenant management is global admin work.
- No `TenantContext` is set.
- Queries intentionally bypass tenant filtering.

### Master Data

Tables involved:

- `vehicle_types`
- `roles`

Vehicle types:

- Global catalog.
- `VehicleType` is not tenant-scoped.
- Delete is a soft delete: `is_active = false`, `is_deleted = true`.

Roles:

- Global identity catalog.
- Loaded from `roles` ordered by name.

Multi-tenant filter behavior:

- `vehicle_types` and `roles` are global tables.
- Tenant filter does not apply.

## 5. Tenant Suspension Trigger: Session Revocation

Suspending a tenant implicitly triggers active session revocation.

Service method:

```java
AdminTenantManagementServiceImpl.toggleTenantStatus(UUID id)
```

When next status is `SUSPENDED`:

1. Query active sessions by tenant:

```java
sessionRepository.findActiveSessionIdsByTenantId(tenantId, now)
```

2. Bulk revoke sessions in PostgreSQL:

```java
sessionRepository.revokeAllActiveByTenantId(tenantId, now)
```

3. For each active session ID, update Redis session state:

```java
sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
sessionAuthorityCacheService.clearAuthz(sessionId);
sessionAuthorityCacheService.clearActive(sessionId);
```

Redis session keys affected:

```text
smartpark:sess:revoked:{sessionId}
smartpark:sess:authz:{sessionId}
smartpark:sess:active:{sessionId}
```

Effect:

- Existing access tokens fail on the next authenticated request.
- Refresh tokens fail because the backing DB session is revoked.
- Redis authz and active markers are cleared to prevent stale authorization.

No Spring Events are used for these endpoints. The implementation uses direct service injection as required.
