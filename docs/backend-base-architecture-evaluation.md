# SmartPark Backend Base Architecture Evaluation

Scan date: 2026-05-20
Scope: aggregated review of `origin/main` and `origin/Branch`

## 1. Branches Reviewed

| Branch | Commit | Notes |
| --- | --- | --- |
| `origin/main` | `f7d3934` | Identity, auth, tenant, Redis cache, rate limit, Docker, GitHub Actions, documentation. |
| `origin/Branch` | `46e728b` | Includes everything from `main` plus dashboard API traffic logging, parking entity/repository, and vehicle type CRUD pieces. |

## 2. Directory Structure Overview

The backend is a Spring Boot 3 / Java 21 Maven service organized by common infrastructure plus business modules.

```text
.
|-- .github/workflows/
|   |-- backend-ci-pr.yaml
|   |-- backend-ci-main.yaml
|   |-- backend-artifact.yaml
|   |-- backend-cd.yaml
|   |-- ai-code-review.yaml
|   `-- ai-docs.yaml
|-- checkstyle/
|-- docs/
|-- scripts/
|-- src/main/java/com/smartpark/swp391/
|   |-- common/
|   |   |-- config/
|   |   |-- exception/
|   |   |-- response/
|   |   `-- security/
|   |-- infrastructure/
|   |   |-- cached/
|   |   `-- persistence/
|   `-- modules/
|       |-- identity/
|       |-- dashboard/        # origin/Branch only
|       |-- parking/          # origin/Branch only
|       `-- vehicle/          # origin/Branch only
|-- src/main/resources/
|   |-- application.yaml
|   |-- db/migration/
|   `-- scripts/token-bucket.lua
|-- compose.yaml
|-- Dockerfile
|-- Makefile
`-- pom.xml
```

## 3. Entity / Model Inventory

### `origin/main`

| Entity | Table | Purpose | Tenant scoped? |
| --- | --- | --- | --- |
| `Tenant` | `tenants` | SaaS tenant/workspace. | Root tenant table. |
| `User` | `users` | Tenant user account. | Yes, direct `tenant_id` FK. |
| `Role` | `roles` | Global role catalog. | No. |
| `Permission` | `permissions` | Global permission catalog. | No. |
| `UserRole` | `user_roles` | User-role join table. | Indirect via `user_id`. |
| `RolePermission` | `role_permissions` | Role-permission join table. | No. |
| `Device` | `devices` | Trusted device record / fingerprint. | Indirect via `user_id`. |
| `Session` | `sessions` | Login session and refresh JTI state. | Indirect via `user_id`; no direct `tenant_id`. |

### Additional entities in `origin/Branch`

| Entity | Table | Purpose | Tenant scoped? |
| --- | --- | --- | --- |
| `ApiTrafficLog` | `api_traffic_logs` | Dashboard request logging. | No tenant field. System-wide telemetry. |
| `Parking` | `parkings` | Parking facility. | Yes, direct `tenant_id` FK in entity. |
| `VehicleType` | `vehicle_types` | Vehicle type catalog. | No tenant field; appears global. |

### Migration coverage

Existing Flyway migrations create `tenants`, identity tables, and seed identity/auth data only:

- `V20260515023500__init_tenant_table.sql`
- `V20260516162100__init_identity_tables.sql`
- `V20260518093000__seed_identity_auth.sql`

`origin/Branch` adds JPA entities for `parkings`, `vehicle_types`, and `api_traffic_logs`, but no Flyway migrations for those tables were found. This will break runtime persistence unless Hibernate DDL auto-generation is enabled outside the reviewed config, which should not be relied on for production.

## 4. Entity Relationships

Current aggregate relationship map:

```text
Tenant 1--N User
Tenant 1--N Parking                  # origin/Branch only

User 1--N Device
User 1--N Session
User N--N Role via UserRole

Role N--N Permission via RolePermission

Device 1--N Session

ApiTrafficLog                         # standalone system telemetry, origin/Branch only
VehicleType                           # standalone catalog, origin/Branch only
```

Important observations:

- `User` is the only identity aggregate with a direct tenant FK.
- `Session` derives tenant through `Session -> User -> Tenant`; it does not embed `tenant_id`.
- `Device` derives tenant through `Device -> User -> Tenant`.
- `Parking` in `origin/Branch` embeds `tenant_id`, but the corresponding database migration is missing.
- No `Slot`, `Invoice`, or parking `Session` business entities exist in either reviewed branch.

## 5. Core Table Tenant FK Verification

Requirement checked: tenant_id must be strictly embedded in core tables: `Parkings`, `Slots`, `Sessions`, `Invoices`.

| Core table | Status | Evidence / risk |
| --- | --- | --- |
| `parkings` | Partially ready in `origin/Branch` | `Parking` entity has `@JoinColumn(name = "tenant_id", nullable = false)`, but no Flyway migration creates `parkings`. Not present in `origin/main`. |
| `slots` | Missing | No `Slot` entity, repository, service, controller, or migration found. |
| `sessions` | Not strictly embedded | Identity `sessions` table has `user_id`, `device_id`, `refresh_jti`, `revoked_at`, `expired_at`; no direct `tenant_id`. Tenant is inferred through `user_id`. |
| `invoices` | Missing | No `Invoice` entity, repository, service, controller, or migration found. |

Conclusion: the base is not yet compliant with strict tenant FK embedding for the required core SaaS data tables.

## 6. Multi-Tenant Middleware / Logical Isolation

No automatic tenant isolation middleware was found in either branch.

Absent mechanisms:

- No Hibernate `@FilterDef`, `@Filter`, `CurrentTenantIdentifierResolver`, or tenant-aware Hibernate interceptor.
- No Spring MVC `HandlerInterceptor` that resolves tenant context and binds it to request scope.
- No repository base class/specification enforcing `WHERE tenant_id = ?`.
- No global query filter for application-level reads.
- No database Row Level Security migration or tenant-aware connection/schema resolver.

Existing tenant-related behavior:

- JWTs include `tenant_id`.
- Authorization cache stores `tenantId`.
- Tenant status is checked during login.
- Tenant suspension in `origin/Branch` revokes active sessions by querying `s.user.tenant.id = :tenantId`.
- Redis key comments mention tenant-oriented eviction, but session keys are keyed by session ID, not tenant.

Readiness evaluation: **not ready for multi-tenant logical isolation**. Tenant identity exists, but data access still depends on each service/repository manually adding tenant predicates. That is high risk for cross-tenant reads once parking, slot, session, invoice, billing, and reporting modules expand.

## 7. Authentication & Zero-Trust Flow

### Login flow

Endpoint: `POST /auth/login`

Flow:

1. `AuthenticationRequest` requires `username`, `password`, and `deviceFingerprint`; `deviceLabel` is optional.
2. `AuthenticationServiceImpl.authenticate()` loads user by username.
3. BCrypt password match is checked.
4. User status and tenant status must both be `ACTIVE`.
5. Device fingerprint is looked up by `(user_id, fingerprint)`.
6. Unknown device is saved as `PENDING`, then login fails with `DEVICE_NOT_TRUST`.
7. Existing device must be `APPROVED`.
8. A `Session` row is created with `user`, `device`, `expiredAt`, `refreshJti`, and null `revokedAt`.
9. Access and refresh JWTs are issued.

### Device fingerprinting

Implemented as client-supplied `deviceFingerprint`, stored on `devices.fingerprint`. The server does not generate or verify a cryptographic device proof. This is an approval-list model, not strong device attestation.

### JWT structure

Access token:

- `sub`
- `iat`
- `exp`
- `user_id`
- `tenant_id`
- `session_id`
- random `jti`

Refresh token:

- Same core claims as access token.
- `typ = REFRESH`
- `jti = sessions.refresh_jti`

Refresh flow rotates the refresh JTI with an atomic repository update:

```text
WHERE session_id = ? AND refresh_jti = ? AND revoked_at IS NULL AND expired_at > CURRENT_TIMESTAMP
```

This is a good replay defense foundation.

### Redis session storage and kill switch

Redis keys:

- `smartpark:sess:authz:{sessionId}` stores `SessionAuthzCache`.
- `smartpark:sess:revoked:{sessionId}` stores revoked markers.
- `smartpark:sess:active:{sessionId}` stores short-lived active markers.
- `smartpark:tenant:detail:{tenantId}` stores tenant cache.
- Rate limit keys exist for user/login throttling.

Request authentication uses `JwtAuthenticationConverter -> SessionAuthorityResolver -> SessionGuardService`.

Kill switch coverage:

- Logout current session revokes DB session and writes Redis revoked marker.
- Logout-all / force logout revokes active sessions by user and marks each session revoked in Redis.
- Tenant suspension in `origin/Branch` revokes active sessions by tenant and marks each session revoked in Redis.
- If Redis is unavailable, guard falls back to DB session status checks.

Gaps:

- `SessionServiceImpl.revokeAll()` uses a hard-coded 15-minute Redis TTL instead of reading configured `jwt.valid-duration`.
- Session Redis keys are not tenant-prefixed, so tenant-wide cache eviction by `RedisKeys.tenantPattern()` will not clear session authz/active/revoked keys.
- `origin/main` tenant suspension only evicts tenant cache; the tenant-wide DB session revoke implementation appears only in `origin/Branch`.

## 8. Business Logic Hooks / Workers / Cronjobs

No worker or cronjob foundation was found in either branch.

Specifically absent:

- No `@EnableScheduling`.
- No `@Scheduled` subscription worker.
- No 00:00 AM subscription renewal/expiration job.
- No subscription module/entity/repository/service.
- No `periodString` logic.
- No logic shifting `periodString` back by one month.
- No notification module.
- No broadcast notification query targeting all linked accounts.
- No owner/account-link model to support "all linked accounts, not just primary owner".

Evaluation: the requested subscription and broadcast hooks are not implemented yet and need new modules, migrations, and tests.

## 9. DevOps & Git Flow

### Docker / Compose

`compose.yaml` runs only dependencies:

- `postgres:16-alpine` on host port `5433`.
- `redis:7-alpine` on host port `6379`.

It does not run the backend application container. That is fine for local development but incomplete for full-stack deployment validation.

`Dockerfile`:

- Multi-stage Java 21 build.
- Maven dependency warmup.
- Spring Boot layered jar extraction.
- Final `eclipse-temurin:21-jre-jammy` image.
- Non-root runtime user.

### CI/CD

CI:

- PR and main workflows run `mvn -B clean test jacoco:report`.

CD / artifact workflows:

- `backend-artifact.yaml` builds and pushes DockerHub image tag `v${{ github.run_number }}` only.
- `backend-cd.yaml` builds and pushes both versioned tags and `latest` tags to DockerHub and GHCR.
- Deployment script receives `TAG="v${GITHUB_RUN_NUMBER}"`, but the workflow still publishes `latest`.

Requirement checked: strict version tagging required; `latest` strictly prohibited.

Result: **non-compliant**. `backend-cd.yaml` explicitly publishes:

- `parking-building-be:latest`
- `ghcr.io/.../parking-building-be:latest`

Immediate fix: remove all `latest` tags and enforce immutable SemVer or run-number tags only. Prefer failing the workflow if the resolved tag is empty, `latest`, or not version-shaped.

## 10. Multi-Tenant Base Readiness Evaluation

Overall status: **foundation exists, but base is not production-ready for multi-tenant SaaS isolation.**

Strengths:

- Tenant entity and status lifecycle exists.
- User has required tenant FK.
- JWT carries tenant context.
- Login blocks suspended tenants.
- Redis-backed session guard and DB fallback exist.
- Refresh token rotation is implemented.
- `origin/Branch` adds tenant session revocation on tenant suspension.
- `Parking` entity in `origin/Branch` correctly models a direct tenant FK.

Major risks:

- No automatic tenant predicate enforcement.
- Required core tables are missing or not tenant-embedded.
- New `origin/Branch` entities lack migrations.
- Global catalogs are not clearly classified as system-owned vs tenant-owned.
- Reporting/dashboard queries are system-wide and not explicitly admin-only at the data-isolation layer.
- Session kill switch is implemented, but tenant-level Redis key design is inconsistent with the tenant eviction comments.

Recommended readiness rating: **4/10** for multi-tenant base readiness.

## 11. Missing Core Files / Modules Needing Immediate Implementation

Priority 0 - isolation and schema correctness:

- Tenant context resolver: request/JWT to `TenantContext`.
- Hibernate tenant filter or repository-level tenant specification base.
- Integration tests proving cross-tenant records are invisible.
- Flyway migrations for `parkings`, `vehicle_types`, and `api_traffic_logs`.
- Decide whether `VehicleType` is global or tenant-specific; add `tenant_id` if tenant-specific.

Priority 1 - required domain core:

- `modules/parking/entity/Slot.java` and migration for `slots(tenant_id, parking_id, ...)`.
- Business parking session module distinct from identity `Session`, e.g. `ParkingSession`.
- Invoice/billing module with `invoices.tenant_id`.
- Subscription module with tenant subscription state.
- Notification module with account-link targeting.
- Account-link/tenant-member model for broadcasting to all linked accounts.

Priority 2 - workers and lifecycle hooks:

- Scheduler configuration with explicit timezone.
- 00:00 subscription worker.
- Subscription period calculation service with tests for month-boundary behavior.
- `periodString` one-month-back logic, including January/year rollover tests.
- Broadcast query that targets all linked accounts, with tests proving it does not target only the primary owner.

Priority 3 - DevOps guardrails:

- Remove `latest` from `backend-cd.yaml`.
- Add CI check that fails when any workflow publishes `latest`.
- Standardize deployment script path; two CD-like workflows currently reference different paths.
- Add app service profile to compose or provide a deployment compose file for backend + Postgres + Redis.

## 12. Executive Summary

SmartPark has a credible identity/security/cache starting point: tenant-aware users, trusted-device login, access/refresh JWTs, refresh rotation, Redis authorization cache, and kill-switch session revocation. `origin/Branch` moves the product toward parking and dashboard modules.

The architecture is not yet a complete multi-tenant SaaS base. Tenant isolation is not automatic, required domain entities are absent, several new Branch entities have no migrations, and CI/CD still publishes prohibited `latest` tags. The next technical milestone should be enforcing tenant isolation centrally before adding more tenant-owned domain repositories.
