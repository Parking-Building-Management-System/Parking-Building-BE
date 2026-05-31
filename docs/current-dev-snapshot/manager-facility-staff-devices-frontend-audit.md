# Manager Facility + Staff & Devices Frontend Audit

Date: 2026-05-31

## Route Coverage Matrix

| Route | Status | API coverage | Notes |
| --- | --- | --- | --- |
| `/manager/facility` | Exists | Real derived APIs | Uses parkings, topology, slots, RFID, and capped floor-map reads for mapped slot count. |
| `/manager/facility/parkings` | Exists | Real APIs | List, create, edit, status toggle, and topology link are wired. Delete is not shown because no supported manager delete endpoint is used. |
| `/manager/facility/floors` | Exists | Real APIs | Parking-scoped list, create, edit, delete are wired. Map action links to floor plans. |
| `/manager/facility/zones` | Exists | Real APIs | Parking/floor scoped list, create, edit, delete are wired. |
| `/manager/facility/slots` | Exists | Real APIs | Filters, create, edit, delete, single status, and bulk status are wired. |
| `/manager/facility/maps` | Exists | Real APIs | Floor map image save/upload and slot coordinate save use real APIs and refetch floor map state. |
| `/manager/facility/slots/import` | Exists | Real APIs | Excel import/export use real manager slot endpoints. |
| `/manager/facility/rfid-cards` | Exists | Real APIs where backend is available | List/filter, generate pool, and status update are wired. UI shows unavailable state if backend returns 404. |
| `/manager/staff-devices` | Exists | Real derived APIs | Staff totals, kiosk totals, pending approvals, approved devices, and readiness are derived without polling. |
| `/manager/staff-devices/staff` | Exists | Real APIs | List/search/status filter, create, edit, status update, and reset password are wired. |
| `/manager/staff-devices/kiosks` | Exists | Real APIs | List/filter, create, edit, status update, delete, assign staff, and unassign staff are wired. |
| `/manager/staff-devices/device-approvals` | Exists | Real APIs | Pending list, approve with kiosk binding, reject, and revoke where device id is present are wired. |
| `/manager/staff-devices/kill-switch` | Exists | API pending page | Kept as a clean pending page because no manager-scoped approved-device/session list API exists. |

## Real API Pages

- Facility overview, parkings, floors, zones, slots, maps, slot import/export, and RFID cards all use manager APIs instead of mock data.
- Staff & Devices overview, staff accounts, kiosks/gates, and device approvals use manager APIs instead of mock data.
- Sidebar Device Approvals badge uses the real device approvals API and counts only `PENDING` items.

## Remaining API Pending Actions

- Manager-level Kill Switch still needs manager-scoped list APIs for approved devices and active staff sessions before destructive actions can be safely shown.
- Facility aggregate summary endpoint is not present; overview derives stats from existing list/topology/map APIs.
- A direct mapped-slot aggregate is not present; overview reads floor maps only when the floor count is within a safe cap.
- RFID single-create, detail, and full update are not implemented because the MVP API supports list, generate, and status update only.
- Active staff sessions are not shown in the Staff & Devices overview because no manager-scoped session aggregate/list API is available.

## Bugs Fixed

- Replaced object-shaped React Query keys in manager staff, kiosk/device, slot, and RFID list keys with primitive stable keys.
- Fixed Device Approvals pending badge/count so paged `totalElements` is not treated as pending count when non-pending rows are present.
- Removed one-request-per-kiosk overview assignment checks; readiness now uses kiosk list staff count fields.
- Added kiosk type and status filters to the Kiosks / Gates page.
- Added `staffCount` fallback for kiosk assigned staff count display.
- Added confirmation prompts for device reject and revoke actions.
- Added RFID generate count validation before calling the backend.
- Removed Vietnamese code comments from the sidebar file; visible UI labels remain English.

## UX Improvements

- Facility overview now includes mapped slots and keeps the map-stat derivation capped to avoid request storms.
- Staff & Devices overview now shows approved devices in addition to total staff, active staff, kiosks, and pending devices.
- Kiosk list filtering now supports parking, kiosk type, and status.
- Device security actions now require explicit confirmation.

## Files Changed

- `src/service/manager/facility-api.ts`
- `src/service/manager/kiosk-device-api.ts`
- `src/service/manager/staff-api.ts`
- `src/components/layout/sidebar.tsx`
- `src/features/manager/facility-overview.tsx`
- `src/features/manager/staff-devices-overview.tsx`
- `src/features/manager/kiosk-management.tsx`
- `src/features/manager/device-approvals.tsx`
- `src/features/manager/rfid-card-management.tsx`
- `docs/current-dev-snapshot/manager-facility-staff-devices-frontend-audit.md`

## Backend Dependencies Before Billing

- Add a manager facility summary endpoint if overview derivation becomes too expensive for large tenants.
- Add manager mapped-slot aggregate if map coverage must be shown without reading floor map documents.
- Add manager approved-device list and active-session list/revoke APIs for Kill Switch.
- Confirm whether `/manager/device-approvals` is pending-only or all statuses; frontend now safely counts pending rows only.
- Keep RFID MVP contract explicit: list, generate pool, and status update only.

## Recommended Next Tasks Before Billing

- Run a browser smoke test against a seeded tenant for all routes in the matrix.
- Verify MinIO/CORS behavior on `/manager/facility/maps` with a real image upload and presigned download.
- Verify kiosk assign/unassign count refresh against backend response fields `assignedStaffCount` or `staffCount`.
- Confirm device approval list semantics and add server-side pending count if pagination grows.
- Only start Pricing & Billing after the above smoke test and backend dependency decisions are closed.
