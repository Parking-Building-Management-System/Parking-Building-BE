# System Admin Navigation Cleanup

## Final Route Map

System Admin sidebar:

- Dashboard: `/admin`
- Tenant Management: `/admin/tenants`
  - All Tenants: `/admin/tenants`
- Master Data: `/admin/master-data`
  - Vehicle Types: `/admin/master-data/vehicle-types`
  - Roles & Permissions: `/admin/master-data/roles-permissions`
- System Health: `/admin/system-health`
  - API Health: `/admin/system-health/api`
  - API Traffic: `/admin/system-health/traffic`
- Audit & Security: `/admin/audit`
  - Audit Logs: `/admin/audit/logs`
  - Force Logout Sessions: `/admin/audit/sessions`

Settings is removed from the System Admin sidebar because there is no real business module behind it yet.

## Removed Duplicate Routes

- Removed injected `Overview` child links for System Admin expandable groups.
- Removed `Tenant Management > Create Tenant` from the sidebar.
- Removed duplicate `Tenant Management > Overview` behavior because it pointed to the same route as `All Tenants`.
- Removed `Master Data > Overview`, `System Health > Overview`, and `Audit & Security > Overview` from visible sidebar children.

Manager and Staff navigation behavior is unchanged. Their overview links remain available where the existing sidebar pattern uses them.

## Master Data Changes

`/admin/master-data` still works and defaults to the Vehicle Types tab.

Tabs:

- Vehicle Types
- Roles & Permissions

Vehicle Types preserves the existing real CRUD flow and table columns:

- Name
- Code
- Status
- Actions

Roles & Permissions keeps the real role list from the existing backend API and adds an API-pending permission definitions section showing the expected tree shape:

```text
Scope -> Module -> Resource -> Label -> Actions
```

Compatibility routes:

- `/admin/master-data/vehicle-types` opens the Vehicle Types tab.
- `/admin/master-data/roles` still works and opens the Roles & Permissions tab.
- `/admin/master-data/roles-permissions` is the new sidebar route.

## Mock Pages Added

Purpose-built mock/API-pending pages were added for:

- `/admin/system-health/api`
- `/admin/system-health/traffic`
- `/admin/audit/logs`
- `/admin/audit/sessions`

These pages do not perform fake backend mutations. Force logout actions are disabled and labeled `Force Logout API pending`.

## API Specs Created

- `docs/api-specs/system-admin-permissions-api.md`
- `docs/api-specs/system-admin-system-health-api.md`
- `docs/api-specs/system-admin-audit-security-api.md`

## English Cleanup Summary

- Sidebar account menu label `Giao diện` changed to `Theme`.
- System Admin sidebar labels are English.
- New System Health, Traffic, Audit Logs, and Sessions pages use English UI labels.
- Backend/user-entered domain values are not translated.

## Pending Backend APIs

- Permission tree and role permission assignment APIs.
- System Health summary, service checks, traffic, top endpoints, and errors APIs.
- Audit log search, cross-tenant session listing, force logout, device listing, and device revoke APIs.

## Files Changed

- `src/config/navigation.ts`
- `src/components/layout/sidebar.tsx`
- `src/features/admin/master-data-config.tsx`
- `src/features/admin/system-health-pages.tsx`
- `src/features/admin/audit-security-pages.tsx`
- `src/app/(protected)/admin/master-data/vehicle-types/page.tsx`
- `src/app/(protected)/admin/master-data/roles/page.tsx`
- `src/app/(protected)/admin/master-data/roles-permissions/page.tsx`
- `src/app/(protected)/admin/system-health/api/page.tsx`
- `src/app/(protected)/admin/system-health/traffic/page.tsx`
- `src/app/(protected)/admin/audit/logs/page.tsx`
- `src/app/(protected)/admin/audit/sessions/page.tsx`
- `docs/api-specs/system-admin-permissions-api.md`
- `docs/api-specs/system-admin-system-health-api.md`
- `docs/api-specs/system-admin-audit-security-api.md`
- `docs/current-dev-snapshot/system-admin-navigation-cleanup.md`

