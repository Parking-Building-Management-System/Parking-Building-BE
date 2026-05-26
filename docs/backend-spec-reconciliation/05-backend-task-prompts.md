# 05. Backend Task Prompts

Each task is intentionally small enough for one Codex session. All tasks must read the reconciliation docs first and must not break existing auth/session contracts.

## Task 1: Audit branch/code reality

### Goal
Confirm current branch reality for admin, parking, vehicle, manager facility, tenant filter, and operation/billing modules.

### Current context to read first
- `docs/backend-spec-reconciliation/00-current-backend-state.md`
- `docs/backend-spec-reconciliation/01-spec-vs-code-gap.md`

### Files likely involved
- Read-only: `src/main/java/**`, `src/main/resources/db/migration/**`, `docs/**`

### Do not touch
- Do not edit code, migrations, or API contracts.

### Implementation requirements
- Produce an updated audit note only if branch reality differs.
- Mark gaps as `MISSING`, `DOC_ONLY`, `ENTITY_WITHOUT_MIGRATION`, or `UNCLEAR`.

### Acceptance criteria
- [ ] Lists real controllers/services/entities/migrations.
- [ ] Identifies stale docs.
- [ ] Includes file paths for every conclusion.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/00-current-backend-state.md` and `01-spec-vs-code-gap.md`, then audit the current branch against `src/main/java`, `src/main/resources/db/migration`, and `docs`. Do not change code or existing API contracts. If reality differs, update only a new markdown note under `docs/backend-spec-reconciliation/` with file-path evidence, statuses using DONE/PARTIAL/DOC_ONLY/ENTITY_WITHOUT_MIGRATION/MISSING/UNCLEAR, acceptance criteria, and verification commands. Do not break auth/session behavior.

## Task 2: Add missing Flyway migrations for existing entities

### Goal
Add migrations only for entities that exist but have no matching Flyway table/columns.

### Current context to read first
- `docs/backend-spec-reconciliation/00-current-backend-state.md`
- `docs/backend-spec-reconciliation/01-spec-vs-code-gap.md`

### Files likely involved
- `src/main/java/com/smartpark/swp391/modules/**/entity/*.java`
- `src/main/resources/db/migration/*.sql`

### Do not touch
- Do not create new domain entities.
- Do not edit old migrations.
- Do not change auth/session contracts.

### Implementation requirements
- Compare every entity/table/column to migrations.
- If missing, create a new Flyway migration only.
- Preserve `ddl-auto: validate`.

### Acceptance criteria
- [ ] Every existing entity validates against Flyway schema.
- [ ] No old migration modified.
- [ ] No new API or entity added.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/00-current-backend-state.md` first. Audit Java entities against Flyway migrations. If an existing entity has missing table/columns, add a new migration only; do not add new entities/APIs and do not modify old migrations. Do not break auth/session behavior or change existing contracts. Acceptance: Hibernate validate passes on startup, tests pass, and the final answer lists exact entity/table fixes. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 3: Implement tenant context and isolation foundation

### Goal
Make tenant context binding reliable for all non-admin tenant-scoped endpoints.

### Current context to read first
- `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md`
- `docs/backend-spec-reconciliation/00-current-backend-state.md`

### Files likely involved
- `src/main/java/com/smartpark/swp391/infrastructure/tenant/*`
- `src/main/java/com/smartpark/swp391/common/security/config/*`
- Manager controllers/services tests

### Do not touch
- Do not rewrite auth/session token format.
- Do not change existing endpoint paths unless necessary.
- Do not implement CRUD domain features in this task.

### Implementation requirements
- Bind `TenantContext` from JWT for manager/staff/user routes.
- Explicitly bypass for `/admin/**` with `SYSTEM_ADMIN`.
- Never trust tenantId from client request.
- Add isolation tests.

### Acceptance criteria
- [ ] Manager tenant A cannot read tenant B facility data.
- [ ] System admin dashboard/tenant list still sees global data.
- [ ] `TenantContext` is cleared after each request.
- [ ] Existing auth/session endpoints still pass.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md` and `00-current-backend-state.md`. Implement only the tenant isolation foundation: request-level `TenantContext` binding from JWT for non-admin tenant-scoped endpoints, explicit admin bypass for `/admin/**`, and tests proving manager tenant A cannot access tenant B while SYSTEM_ADMIN global endpoints still work. Do not implement new CRUD domains, do not trust client tenantId, and do not break auth/session contracts. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 4: Implement System Admin tenant provisioning contract

### Goal
Align tenant provisioning on `/admin/tenants` if current contract is incomplete.

### Current context to read first
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/admin-api-spec.md`

### Files likely involved
- `AdminTenantManagementController.java`
- `AdminTenantManagementServiceImpl.java`
- Tenant/User/Role repositories and DTOs

### Do not touch
- Do not remove legacy `/tenants` unless owner explicitly approves.
- Do not change JWT/session format.

### Implementation requirements
- SYSTEM_ADMIN only.
- Create tenant and initial PARKING_MANAGER.
- Revoke sessions when tenant suspended.
- Keep existing response envelope.

### Acceptance criteria
- [ ] `/admin/tenants` list/create/toggle status works.
- [ ] Provisioned manager can login from approved device only according to current auth rules.
- [ ] Suspension revokes tenant sessions.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md` and `docs/admin-api-spec.md`. Check `/admin/tenants` against the intended System Admin tenant provisioning contract. Implement only missing pieces for list/create/toggle status and initial PARKING_MANAGER provisioning. Do not remove legacy `/tenants`, do not break auth/session behavior, and do not change contracts unless needed. Acceptance: SYSTEM_ADMIN-only access, tenant suspension revokes sessions, tests pass. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 5: Implement Vehicle Type CRUD

### Goal
Ensure global Vehicle Type CRUD is complete and documented for FE.

### Current context to read first
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/admin-api-spec.md`

### Files likely involved
- `AdminMasterDataController.java`
- `AdminMasterDataServiceImpl.java`
- `VehicleType.java`
- `VehicleTypeRepository.java`

### Do not touch
- Do not make VehicleType tenant-specific unless owner decides.
- Do not change auth/session contracts.

### Implementation requirements
- SYSTEM_ADMIN only.
- Global master data.
- Soft delete/inactivate.
- Cache invalidation on writes.

### Acceptance criteria
- [ ] List/create/update/delete vehicle types work.
- [ ] Duplicate code returns conflict.
- [ ] Cache evicts on mutation.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md` and `docs/admin-api-spec.md`. Complete or verify global Vehicle Type CRUD under `/admin/master-data/vehicle-types`. Keep VehicleType global, preserve existing response envelope and auth/session behavior, and do not change API contracts unnecessarily. Acceptance: SYSTEM_ADMIN only, duplicate code conflict, soft delete/inactivate, cache eviction on writes. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 6: Implement Parking/Floor/Zone/Slot CRUD

### Goal
Complete tenant-scoped facility CRUD for manager screens.

### Current context to read first
- `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md`
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/manager-facility-api-spec.md`

### Files likely involved
- `ManagerFacilityController.java`
- `ManagerSlotController.java`
- `ManagerFacilityServiceImpl.java`
- `ManagerSlotServiceImpl.java`
- Parking/Floor/Zone/Slot repositories and DTOs

### Do not touch
- Do not implement parking sessions/billing/pricing here.
- Do not trust tenantId from request.
- Do not break existing manager API contracts.

### Implementation requirements
- Tenant scope from JWT/session.
- Preserve topology cache invalidation.
- Add only missing parking and single slot CRUD if needed.

### Acceptance criteria
- [ ] Manager can CRUD facility records in own tenant only.
- [ ] Tenant A cannot access tenant B resources.
- [ ] Existing import/export/search still work.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md`, `04-crud-first-phase-scope.md`, and `docs/manager-facility-api-spec.md`. Implement only missing tenant-scoped Parking/Floor/Zone/Slot CRUD needed by the manager facility UI, preserving existing contracts for manager endpoints. Do not implement parking sessions, pricing, billing, or PWA. Do not trust tenantId from clients and do not break auth/session. Acceptance: manager can operate only within JWT tenant, topology cache invalidates, tests pass. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 7: Implement Manager Staff basic CRUD

### Goal
Allow PARKING_MANAGER to create/list/toggle STAFF accounts using internal username.

### Current context to read first
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/backend-spec-reconciliation/06-questions-for-owner.md`

### Files likely involved
- New manager staff controller/service/dto
- `User.java`, `Role.java`, `UserRole.java`
- `UserRepository.java`, `RoleRepository.java`, `UserRoleRepository.java`
- `SessionRepository.java`

### Do not touch
- Do not add social login.
- Do not change auth/session token format.
- Do not create staff in another tenant.

### Implementation requirements
- Manager role only.
- Staff tenant is current manager JWT tenant.
- Assign STAFF role.
- Toggle inactive should revoke active sessions.

### Acceptance criteria
- [ ] Manager creates STAFF with internal username.
- [ ] Manager lists only own tenant staff.
- [ ] Inactive staff cannot login.
- [ ] Toggling inactive revokes staff sessions.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md` and `06-questions-for-owner.md`. Implement manager-scoped STAFF basic CRUD with current JWT tenant, internal username, STAFF role assignment, list own-tenant staff only, and toggle active/inactive with session revoke. Do not add social login, do not alter auth/session token format, and do not change existing contracts unnecessarily. Acceptance criteria and tests must prove tenant isolation and inactive-login denial. Verify with `./mvnw test` and `./mvnw spring-boot:run`.

## Task 8: Implement Device Approval basic flow

### Goal
Support unknown staff device -> pending -> manager approve/reject -> staff login again.

### Current context to read first
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md`

### Files likely involved
- `Device.java`
- `DeviceRepository.java`
- `AuthenticationServiceImpl.java`
- New manager device controller/service/dto

### Do not touch
- Do not weaken login device checks.
- Do not break refresh/logout/session behavior.
- Do not implement kiosk hardware integration.

### Implementation requirements
- Pending list scoped to manager tenant.
- Approve/reject by manager.
- Temporary 8-hour approval may use `expiresAt` if owner accepts MVP.
- Revoke device should revoke active sessions for that device.

### Acceptance criteria
- [ ] Unknown device login creates PENDING and returns `DEVICE_NOT_TRUST`.
- [ ] Manager sees only own-tenant pending devices.
- [ ] Approved device can login.
- [ ] Rejected/revoked device cannot login.
- [ ] Revoking device kills active sessions on that device.

### Verify commands
```bash
./mvnw test
./mvnw spring-boot:run
```

### Suggested Codex Prompt
Read `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md` and `03-multi-tenant-isolation-plan.md`. Implement only the basic device approval flow: unknown login creates PENDING, manager lists own-tenant pending devices, manager approve/reject, approved login succeeds, rejected/revoked login fails, revoke kills active sessions for that device. Do not weaken existing login checks, do not break refresh/logout/session behavior, and do not add hardware integration. Verify with `./mvnw test` and `./mvnw spring-boot:run`.
