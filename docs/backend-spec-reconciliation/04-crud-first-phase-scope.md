# 04. CRUD First Phase Scope

Goal: ship backend contracts that let frontend build SaaS admin and manager facility screens without pulling in complex billing/session operations too early.

## PHASE 1A - SYSTEM_ADMIN

| Priority | API | Role | Entity Needed | Migration Needed? | Existing Code | FE Value | Complexity | Notes |
|---|---|---|---|---|---|---|---|---|
| P0 | `GET /admin/dashboard/stats` | SYSTEM_ADMIN | Tenant, Parking, ApiTrafficLog | No | DONE | SaaS landing dashboard | Low | Already exists; confirm traffic writer/health expectations. |
| P0 | `GET /admin/tenants` | SYSTEM_ADMIN | Tenant | No | DONE | Tenant management list | Low | Already paginated. |
| P0 | `POST /admin/tenants` | SYSTEM_ADMIN | Tenant, User, Role, UserRole | No | DONE | Tenant provisioning with manager | Low | Existing contract uses manager email and initial password. |
| P0 | `PATCH /admin/tenants/{id}/status` | SYSTEM_ADMIN | Tenant, Session | No | DONE | Suspend/reactivate tenant | Low | Existing suspension revokes sessions. |
| P0 | `GET /admin/master-data/vehicle-types` | SYSTEM_ADMIN | VehicleType | No | DONE | Vehicle type config screen | Low | Cached. |
| P0 | `POST /admin/master-data/vehicle-types` | SYSTEM_ADMIN | VehicleType | No | DONE | Create global type | Low | Existing. |
| P0 | `PUT /admin/master-data/vehicle-types/{id}` | SYSTEM_ADMIN | VehicleType | No | DONE | Edit global type | Low | Existing. |
| P0 | `DELETE /admin/master-data/vehicle-types/{id}` | SYSTEM_ADMIN | VehicleType | No | DONE | Soft delete/inactivate type | Low | Existing. |
| P1 | `GET /admin/master-data/roles` | SYSTEM_ADMIN | Role | No | DONE | Role dropdown | Low | Existing read-only role list. |
| P2 | Role/permission CRUD | SYSTEM_ADMIN | Role, Permission | No | PARTIAL | Full RBAC UI | Medium | Do not build until owner confirms RBAC editor rules. |

## PHASE 1B - PARKING_MANAGER Facility

| Priority | API | Role | Entity Needed | Migration Needed? | Existing Code | FE Value | Complexity | Notes |
|---|---|---|---|---|---|---|---|---|
| P0 | `GET /manager/parkings` | PARKING_MANAGER | Parking, Slot | No | DONE | Parking selector/list | Low | Existing. |
| P0 | `PATCH /manager/parkings/{id}/status` | PARKING_MANAGER | Parking | No | DONE | Toggle active/maintenance | Low | Existing. |
| P0 | `GET /manager/parkings/{id}/topology` | PARKING_MANAGER | Parking, Floor, Zone, Slot | No | DONE | Facility tree/map | Low | Existing cached topology. |
| P0 | Floor CRUD | PARKING_MANAGER | Floor | No | DONE | Manage floor structure | Low | Existing create/get/list/update/delete. |
| P0 | Zone CRUD | PARKING_MANAGER | Zone, VehicleType | No | DONE | Manage zones by vehicle type | Low | Existing create/get/list/update/delete. |
| P0 | `GET /manager/slots` | PARKING_MANAGER | Slot | No | DONE | Slot table/search/filter | Low | Existing paginated search. |
| P1 | `PATCH /manager/slots/bulk-status` | PARKING_MANAGER | Slot | No | DONE | Mass maintenance/lock | Low | Existing; allowed statuses restricted. |
| P1 | `POST /manager/slots/import` | PARKING_MANAGER | Slot | No | DONE | Bulk onboarding | Medium | Existing Excel import. |
| P1 | `GET /manager/slots/export` | PARKING_MANAGER | Slot | No | DONE | Audit/export | Low | Existing Excel export. |
| P1 | Parking create/update/delete | PARKING_MANAGER or SYSTEM_ADMIN | Parking | No | MISSING | Facility onboarding | Medium | Requires owner decision: who owns parking creation. |
| P1 | Single Slot create/update/delete | PARKING_MANAGER | Slot | No | MISSING | Manual slot edits | Medium | Needed if frontend has row-level CRUD, despite import existing. |

## PHASE 1C - Staff Account / Device Basics

| Priority | API | Role | Entity Needed | Migration Needed? | Existing Code | FE Value | Complexity | Notes |
|---|---|---|---|---|---|---|---|---|
| P0 | Manager create staff | PARKING_MANAGER | User, Role, UserRole | No | MISSING | Staff account screen | Medium | Must use tenant from JWT; username format needs owner answer. |
| P0 | Manager list staff | PARKING_MANAGER | User, Role | No | MISSING | Staff management | Medium | Must scope tenant and role STAFF. |
| P0 | Manager toggle staff active | PARKING_MANAGER | User, Session | No | MISSING | Inactive/kill switch | Medium | Should revoke sessions on inactive. |
| P0 | Device pending request list | PARKING_MANAGER | Device | Maybe no | MISSING | Approve kiosk/device | Medium | Can initially use `devices.status=PENDING`; separate request entity can wait if MVP. |
| P0 | Approve/reject device request | PARKING_MANAGER | Device | Maybe no | MISSING | Unblock staff login | Medium | Set `approved_by`, `approved_at`, `status`, maybe `expires_at`. |
| P1 | Revoke device | PARKING_MANAGER | Device, Session | No | MISSING | Remove compromised device | Medium | Should revoke sessions for that device. |
| P1 | Force logout user | PARKING_MANAGER | Session | No | PARTIAL | Kill switch | Medium | Admin endpoint exists; manager-scoped endpoint MISSING. |

## Recommendation

Do first:

1. PHASE_0_FOUNDATION tenant binding tests/fix before any new tenant-scoped CRUD.
2. Complete missing Phase 1 gaps that unblock frontend: staff CRUD, device approval, manager kill switch, parking/slot single CRUD if frontend needs them.
3. Keep existing admin and manager APIs; do not rename contracts without frontend coordination.

Do not do now:

- Pricing calculation, fast cash, PWA checkout, subscription renewal jobs, debt notification, analytics heatmaps.
- Incident/red-flag full workflow until operations entry/exit exists.
- Real payment gateway integration.

Must wait for frontend spec:

- Whether parking and slot single CRUD are needed beyond list/toggle/import/export.
- Staff management table filters and fields.
- Device approval UI: device-level only or request-level history.
- Kiosk/gate assignment UI.

Must wait for DB/design decision:

- Whether to add `DeviceApprovalRequest` now or use `Device.expiresAt` for MVP.
- Whether PWA users reuse `users` or separate `DriverAccount`.
- Whether physical card is current `rfid_cards` or a new `parking_cards` table.

Must wait for multi-tenant foundation:

- Any new manager/staff/user endpoint touching business tables.
- Staff CRUD and device approvals, because they must not cross tenants.
