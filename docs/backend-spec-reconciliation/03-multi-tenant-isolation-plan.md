# 03. Multi-Tenant Isolation Plan

## Current Analysis

JWT has `tenant_id`: DONE. `TokenServiceImpl` writes `tenant_id` into access and refresh tokens.

Tenant context exists: DONE. `TenantContext` is a `ThreadLocal<UUID>`.

Filter/aspect exists: PARTIAL. `TenantScopedEntity` defines `tenantFilter`; tenant-scoped entities add `@Filter`; `TenantFilterAspect` enables the filter around repository calls when `TenantContext` exists and disables it when context is empty.

Request binding exists only for manager facility APIs: PARTIAL. `ManagerTenantContext` extracts `tenant_id` from JWT and wraps manager service calls. No global MVC filter/interceptor was found for Manager/Staff/User endpoints.

Repository tenant enforcement is mixed: PARTIAL. Some bulk query methods explicitly pass `tenantId`, for example `SlotRepository.bulkUpdateStatus`. Most tenant filtering relies on Hibernate filter being enabled before repository access.

System Admin bypass exists by convention: PARTIAL. Admin controllers do not set `TenantContext`, so `TenantFilterAspect` disables tenant filtering. This allows global admin list/count operations. This is useful, but it also means non-admin endpoints must never forget to bind context.

Client-supplied tenant is not used in manager facility APIs: DONE for current manager APIs. `ManagerTenantContext` takes tenant from JWT, not request body/query.

## Options

| Option | Summary | Pros | Cons | Fit Now |
|---|---|---|---|---|
| Option A: Hibernate Filter | Use `TenantScopedEntity` + `@Filter`, enable per request from JWT. | Already partially implemented; low code churn; protects many repository methods automatically. | Easy to bypass if context not set; native queries need care; admin bypass must be explicit. | Best current path if request binding is fixed globally. |
| Option B: Service-level tenant predicate mandatory | Every service/repository method manually includes `tenantId`. | Very explicit; simple to reason in code review. | High repetition; easy to miss; weak for generic `findById`; slows CRUD delivery. | Useful as extra guard for write/bulk methods, not enough alone. |
| Option C: Base tenant-scoped repository/specification | Standard repository helpers require tenant predicate. | Consistent query API and testable conventions. | Requires refactor and discipline; does not automatically cover all JPA paths. | Good later hardening after foundation. |
| Option D: Database RLS | PostgreSQL row-level security with tenant session variable. | Strongest DB-enforced isolation. | More operational complexity; app must set DB session variable; admin bypass and migrations harder. | Phase later, not first move. |

## Recommendation

Choose Option A for the current phase: keep Hibernate filter, but promote tenant binding from `ManagerTenantContext` wrappers to a global request-level mechanism for all non-admin authenticated endpoints. Keep Option B explicit predicates for dangerous write/bulk operations. Consider Option C later for repository consistency. Defer Option D until the SaaS model stabilizes.

Reason: the code already has `TenantScopedEntity`, `@Filter`, and `TenantFilterAspect`. The main defect is not the filter primitive; it is incomplete request binding. Fixing that is smaller and safer than redesigning persistence while facility APIs are still being built.

## Target Rules

- System Admin global endpoints under `/admin/**` bypass tenant filter intentionally.
- Manager/Staff/User endpoints must get tenant from JWT/session only.
- Backend must ignore or reject tenantId in request body/query for tenant-scoped operations.
- Frontend must not choose tenant arbitrarily.
- Every core business table should have direct `tenant_id`, including future `pricing_policies`, `payments`, `staff_shifts`, `shift_cash_drops`, `incidents`, `red_flag_actions`, `exit_passes`.
- Tests must prove manager/staff/user cross-tenant denial and system admin global access.

| Concern | Current | Target | Implementation Files | Risk |
|---|---|---|---|---|
| JWT tenant claim | `tenant_id` claim exists in tokens. | Required for all non-system users; validated before tenant-scoped request. | `TokenServiceImpl.java`, `JwtAuthenticationConverter.java` | If missing/malformed, endpoint must reject with 401. |
| TenantContext binding | Only manager wrappers set context. | Global filter/interceptor sets context for non-admin authenticated requests and clears in `finally`. | New security/web filter near `common/security` or `infrastructure/tenant`; existing `ManagerTenantContext.java` can be retired later. | Forgetting to clear ThreadLocal can leak tenant across requests. |
| Hibernate filter | Aspect enables/disables around repositories. | Enabled for tenant-scoped requests; disabled only for approved global admin contexts. | `TenantFilterAspect.java`, `TenantScopedEntity.java` | Empty context currently disables filter silently. |
| Admin bypass | Admin controllers naturally bypass because no context is set. | Bypass must be explicit by route/role, e.g. `/admin/**` + `ROLE_SYSTEM_ADMIN`. | `SecurityConfig.java`, new tenant request filter | Accidental bypass for non-admin route if route matcher is broad. |
| Repository reads | Many methods rely on Hibernate filter. | All tenant-scoped repository calls protected by enabled filter; critical writes also pass tenantId. | `ParkingRepository.java`, `FloorRepository.java`, `ZoneRepository.java`, `SlotRepository.java` | Native queries and projections can bypass entity filter. |
| Bulk writes | `SlotRepository.bulkUpdateStatus` includes `tenantId`. | Continue explicit tenant predicate for all bulk update/delete. | `SlotRepository.java`, future operation repositories | Bulk updates without tenant predicate can modify other tenant data. |
| Staff/User operations | MISSING endpoints. | All future controllers inherit global tenant binding. | Future staff/PWA controllers | Highest future leak risk if built before foundation. |
| Core table tenant_id | Many current core tables have direct `tenant_id`. Identity `sessions` is indirect via user. | Business core tables direct `tenant_id`; identity sessions may remain indirect if all session queries join user when tenant-wide. | Migrations `V20260520100000...`, `V20260516162100...` | Some spec language says sessions core; decide if identity `sessions` needs direct tenant_id or is acceptable indirect. |
| Client tenantId | Current manager APIs do not accept tenantId. | Reject tenantId for scoped operations unless admin provisioning endpoint. | DTO validation and controller review | Client-chosen tenant is a security bug. |
| Tests | Minimal context test not found. | Add isolation tests before expanding CRUD. | Integration tests under `src/test/java` | Without tests, regressions are likely. |

## Required Tests

- Manager from tenant A cannot list, read, update, or delete Parking/Floor/Zone/Slot from tenant B.
- Staff from tenant A cannot create/complete session or update slot from tenant B.
- User/PWA from tenant A cannot read active session/invoice/subscription from tenant B.
- System Admin can call `/admin/tenants`, `/admin/dashboard/stats`, and master-data endpoints without tenant filter hiding global data.
- A request containing `tenantId` in body/query cannot override JWT tenant.
- A repository call under empty `TenantContext` is allowed only for explicitly global/admin service paths.
