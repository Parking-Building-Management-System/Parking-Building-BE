# Manager Facility + Staff Devices Backend Audit

Audit date: 2026-05-31

Scope: Manager Facility Setup and Staff & Devices only. Pricing and Billing were not started.

## Response Conventions

JSON endpoints return the shared wrapper:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {},
  "timestamp": "2026-05-31T00:00:00Z",
  "path": "/manager/example"
}
```

Paged endpoints return `result.content`, `result.page`, `result.size`, `result.totalElements`,
and `result.totalPages`. Current paged Manager endpoints are staff, slots, and RFID cards.

All Manager endpoints require `Authorization: Bearer <manager-access-token>` and
`ROLE_PARKING_MANAGER`. Tenant scope is resolved from the JWT by `ManagerTenantContext`; FE must
not send `tenantId`.

## Facility API Matrix

| Domain | Action | Method | Path | Status | Request / Filters | Response |
|---|---|---:|---|---|---|---|
| Parking | list | GET | `/manager/parkings` | Ready | none | `ParkingResponse[]` with `totalCapacity` derived from slots |
| Parking | create | POST | `/manager/parkings` | Ready | `{code,name,address,status}` | `ParkingResponse` |
| Parking | detail | GET | `/manager/parkings/{id}` | Ready | path id | `ParkingResponse` |
| Parking | update | PUT | `/manager/parkings/{id}` | Ready | `{code,name,address,status}` | `ParkingResponse` |
| Parking | status update / toggle | PATCH | `/manager/parkings/{id}/status` | Ready | body optional; `{status}` if present | `{id,status}` |
| Parking | topology | GET | `/manager/parkings/{id}/topology` | Ready | path id | parking with nested floors and zones, zone `slotCount` |
| Floor | list by parking | GET | `/manager/parkings/{parkingId}/floors` | Ready | path parkingId | `FloorResponse[]` |
| Floor | create | POST | `/manager/parkings/{parkingId}/floors` | Ready | `{code,name,displayOrder,active}` | `FloorResponse` |
| Floor | detail | GET | `/manager/floors/{id}` | Ready | path id | `FloorResponse` |
| Floor | update | PUT | `/manager/floors/{id}` | Ready | `{code,name,displayOrder,active}` | `FloorResponse` |
| Floor | soft delete | DELETE | `/manager/floors/{id}` | Ready | empty floor only | `result: null` |
| Floor | map setup | PATCH | `/manager/floors/{id}/map` | Ready | `{mapImageUrl}` | `FloorMapResponse` |
| Zone | list by floor | GET | `/manager/floors/{floorId}/zones` | Ready | path floorId | `ZoneResponse[]` |
| Zone | create | POST | `/manager/floors/{floorId}/zones` | Ready | `{code,name,vehicleTypeCode,capacity,status}` | `ZoneResponse` |
| Zone | detail | GET | `/manager/zones/{id}` | Ready | path id | `ZoneResponse` |
| Zone | update | PUT | `/manager/zones/{id}` | Ready | `{code,name,vehicleTypeCode,capacity,status}` | `ZoneResponse` |
| Zone | soft delete | DELETE | `/manager/zones/{id}` | Ready | empty zone only | `result: null` |
| Slot | list/search/filter | GET | `/manager/slots` | Ready | `zoneId`, `status`, `slotCode`, `exact`, `page`, `size` | paged `SlotResponse` |
| Slot | create | POST | `/manager/zones/{zoneId}/slots` | Ready | `{code,slotNumber,status}` | `SlotResponse` |
| Slot | detail | GET | `/manager/slots/{id}` | Ready | path id | `SlotResponse` |
| Slot | update | PUT | `/manager/slots/{id}` | Ready | `{code,slotNumber,status}` | `SlotResponse` |
| Slot | soft delete | DELETE | `/manager/slots/{id}` | Ready | path id | `result: null` |
| Slot | status update | PATCH | `/manager/slots/{id}/status` | Ready | `{status}` | `SlotResponse` |
| Slot | bulk status | PATCH | `/manager/slots/bulk-status` | Ready | `{slotIds,newStatus}` | `{updatedCount,newStatus}` |
| Slot | import | POST | `/manager/slots/import` | Ready | multipart `file`; headers `parkingCode,floorCode,zoneCode,slotCode` | `{insertedCount}` |
| Slot | export | GET | `/manager/slots/export` | Ready | none | Excel attachment |
| Slot | coordinate update | PATCH | `/manager/slots/{id}/coordinate` | Ready | `{xCoordinate,yCoordinate}` from 0 to 100 | `SlotCoordinateResponse` |
| Slot | bulk coordinate update | PATCH | `/manager/slots/coordinates` | Ready | `{items:[{slotId,xCoordinate,yCoordinate}]}` | `{updatedCount}` |
| RFID Card | list/filter | GET | `/manager/rfid-cards` | Ready | `status`, `page`, `size` | paged `RfidCardResponse` |
| RFID Card | generate pool | POST | `/manager/rfid-cards/generate` | Ready | optional `{count,prefix}` | `{requestedCount,createdCount,existingCount}` |
| RFID Card | status update | PATCH | `/manager/rfid-cards/{id}/status` | Ready | `{status}` | `RfidCardResponse` |
| RFID Card | detail | GET | `/manager/rfid-cards/{id}` | Missing | not implemented | FE can use list by id client-side for now |
| RFID Card | create single | POST | `/manager/rfid-cards` | Not supported | not implemented | pool generation only |
| RFID Card | update metadata | PUT | `/manager/rfid-cards/{id}` | Not supported | not implemented | status update only |
| Facility Map | get floor map | GET | `/manager/floors/{id}/map` | Ready | path id | floor map plus slot coordinate list |
| Facility Map | presign upload | POST | `/manager/storage/presign-upload` | Ready | `{fileName,contentType,folder}` | upload URL, object key, public URL |
| Facility Map | presign download | GET | `/manager/storage/presign-download` | Ready | `objectKey` | download URL |
| Demo map seed | startup seed | config runner | `app.demo-seed.facility-map` | Ready | app config | seeds demo maps and coordinates |

## Staff Devices API Matrix

| Domain | Action | Method | Path | Status | Request / Filters | Response |
|---|---|---:|---|---|---|---|
| Staff Accounts | list/search/filter | GET | `/manager/staff` | Ready | `search`, `status`, `page`, `size` | paged `ManagerStaffResponse` |
| Staff Accounts | create | POST | `/manager/staff` | Ready | `{username,password|initialPassword,fullName,phone,status}` | `ManagerStaffResponse` |
| Staff Accounts | detail | GET | `/manager/staff/{id}` | Ready | path id | `ManagerStaffResponse` |
| Staff Accounts | update | PUT | `/manager/staff/{id}` | Ready | `{fullName,phone,status}` | `ManagerStaffResponse` |
| Staff Accounts | status update | PATCH | `/manager/staff/{id}/status` | Ready | `{status}` | `ManagerStaffResponse` |
| Staff Accounts | reset password | POST | `/manager/staff/{id}/reset-password` | Ready | `{newPassword}` | `ManagerStaffResponse` |
| Staff Accounts | revoke on deactivate/reset | service behavior | n/a | Ready | deactivate/suspend/reset password | active sessions revoked |
| Kiosks / Gates | list/filter | GET | `/manager/kiosks` | Ready | `parkingId`, `status`, `type` | `ManagerKioskResponse[]` with `assignedStaffCount` |
| Kiosks / Gates | create | POST | `/manager/kiosks` | Ready | `{parkingId,name,type,status}` | `ManagerKioskResponse` |
| Kiosks / Gates | detail | GET | `/manager/kiosks/{id}` | Ready | path id | `ManagerKioskResponse` |
| Kiosks / Gates | update | PUT | `/manager/kiosks/{id}` | Ready | `{name,type,status}` | `ManagerKioskResponse` |
| Kiosks / Gates | status update | PATCH | `/manager/kiosks/{id}/status` | Ready | `{status}` | `ManagerKioskResponse` |
| Kiosks / Gates | soft delete | DELETE | `/manager/kiosks/{id}` | Ready | path id | sets deleted and inactive |
| Kiosk Staff | assignment list | GET | `/manager/kiosks/{id}/staff` | Ready | path kiosk id | `ManagerKioskStaffResponse[]` |
| Kiosk Staff | assign staff | POST | `/manager/kiosks/{id}/staff/{staffId}` | Ready | path ids | `ManagerKioskStaffResponse` |
| Kiosk Staff | unassign staff | DELETE | `/manager/kiosks/{id}/staff/{staffId}` | Ready | path ids | `result: null` |
| Device Approvals | list pending | GET | `/manager/device-approvals` | Ready | none | pending `ManagerDeviceResponse[]` |
| Device Approvals | list approved/rejected | GET | `/manager/devices?status=` | Missing | not implemented | recommend Manager device list |
| Device Approvals | approve | POST | `/manager/device-approvals/{id}/approve` | Ready | `{kioskId,expiresAt}` | approved `ManagerDeviceResponse` |
| Device Approvals | reject | POST | `/manager/device-approvals/{id}/reject` | Ready | none | rejected `ManagerDeviceResponse` |
| Device Approvals | revoke device | POST | `/manager/devices/{id}/revoke` | Ready partial | path id | sets status `SUSPENDED` and clears kiosk |
| Kill Switch | list active staff sessions | GET | n/a | Missing | data model exists | recommend Manager scoped endpoint |
| Kill Switch | revoke staff sessions | POST | n/a | Missing | service foundation exists | recommend Manager scoped endpoint |
| Kill Switch | revoke one session | POST | n/a | Missing | repository foundation exists | recommend Manager scoped endpoint |
| Kill Switch | revoke kiosk device and sessions | POST | `/manager/devices/{id}/revoke` | Partial | no session revoke | device revoked; sessions remain until auth/session guard rejects |

## Validation And Isolation Findings

- Tenant scoping is consistently applied through `ManagerTenantContext` plus tenant-scoped repository
  lookups for parking, floor, zone, slot, staff, kiosk, and device approval APIs.
- Parking/floor/zone/slot duplicate code validation exists within the relevant parent scope.
- Soft delete exists for floors, zones, slots, and kiosks. Floor delete requires no zones; zone
  delete requires no slots. Slot delete does not check active sessions yet.
- Status enum validation is handled by Spring enum binding and DTO `@NotNull` where required.
- Staff creation always assigns only the `STAFF` role. Staff queries require tenant plus `STAFF`.
- Kiosk creation validates parking ownership. Kiosk assignment validates staff tenant plus `STAFF`.
- Staff login requires approved device, kiosk binding, active kiosk, same tenant, and active staff
  assignment. `GET /auth/me` returns `workContext` for staff.
- Device approval validates device tenant, kiosk tenant, active staff assignment to kiosk, and future
  `expiresAt` when present.
- Fixed during this audit: slot import now resolves `parkingCode` inside the current manager tenant,
  and bulk slot status rejects cross-tenant slot IDs before update.

## Overview Data For FE

Facility overview has no dedicated endpoint. FE can derive:

- `totalParkings` from `GET /manager/parkings.length`
- `totalFloors` from summing `GET /manager/parkings/{id}/topology.floors.length`
- `totalZones` from topology nested zones
- `totalSlots` from parking `totalCapacity` or topology zone `slotCount`
- `mappedSlots` from `GET /manager/floors/{id}/map.slots[*].hasCoordinate`
- `rfidCards` from `GET /manager/rfid-cards?page=0&size=1.totalElements`

Staff & Devices overview has no dedicated endpoint. FE can derive:

- staff count from `GET /manager/staff?page=0&size=1.totalElements`
- active staff count from `GET /manager/staff?status=ACTIVE&page=0&size=1.totalElements`
- kiosk count from `GET /manager/kiosks.length`
- pending devices count from `GET /manager/device-approvals.length`
- approved devices count is not available to Manager without a device list endpoint
- active sessions count is not available to Manager without a kill-switch/session endpoint

Sidebar badges: pending approvals can use `GET /manager/device-approvals` and count the array.
Recommended improvement: `GET /manager/device-approvals/count?status=PENDING` or a compact
`GET /manager/staff-devices/summary`.

## Request Samples

Create parking:

```json
{"code":"HQ","name":"Headquarters Parking","address":"1 Nguyen Hue","status":"ACTIVE"}
```

Create slot:

```json
{"code":"B1-A-001","slotNumber":"001","status":"AVAILABLE"}
```

Bulk slot status:

```json
{"slotIds":["11111111-1111-1111-1111-111111111111"],"newStatus":"MAINTENANCE"}
```

Approve device:

```json
{"kioskId":"22222222-2222-2222-2222-222222222222","expiresAt":null}
```

Reset staff password:

```json
{"newPassword":"NewPassword@123"}
```

## Known Limitations / Missing Endpoints

- No Manager Facility overview or Staff & Devices summary endpoint.
- No RFID detail, single create, or metadata update endpoint.
- No Manager device history/list endpoint for approved, rejected, or suspended staff devices.
- No Manager active session list or per-session revoke endpoint.
- Manager device revoke does not revoke active sessions for that device or user.
- No dedicated pending-device count endpoint for sidebar badges.
- Slot list filters by `zoneId`; it does not expose direct `parkingId` or `floorId` filters.
- Facility map accepts external `http(s)` URLs without backend fetch/scanning.
- Check-in still accepts `parkingId` as a DEV fallback while FE migrates to kiosk context.

## Recommended Next Backend Tasks

1. Add `GET /manager/staff-devices/summary` with staff, kiosk, device, and session counts.
2. Add `GET /manager/devices?status=&staffId=&kioskId=&page=&size=` for Manager device history.
3. Extend `POST /manager/devices/{id}/revoke` to revoke active sessions for that device or staff user.
4. Add `GET /manager/sessions?staffId=&status=&page=&size=` and revoke endpoints for Kill Switch.
5. Add `GET /manager/facility/summary` if FE needs one call for overview cards.
6. Add RFID detail endpoint if FE needs direct deep-link/detail pages.

## Bugs Fixed In This Audit

- `ManagerSlotServiceImpl.ImportLookup.parking` used global `parkingCode` lookup. It now uses
  `findByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(currentTenantId(), code)`.
- `ManagerSlotServiceImpl.bulkUpdateStatus` now rejects any loaded slot whose `tenant.id` differs
  from the current Manager tenant before running the tenant-scoped bulk update.

## Swagger Tags

Manager endpoints are annotated under:

- `Manager Facility`
- `Manager Slots`
- `Manager Facility Map`
- `Manager Storage`
- `Manager RFID Cards`
- `Manager Staff Accounts`
- `Manager Kiosks`
- `Manager Kiosk Staff`
- `Manager Device Approvals`

Swagger should expose these paths once the application starts and `/v3/api-docs` is reachable.

## FE Wiring Notes

- Use `result.content`, not `result.items`, for paged responses.
- `PATCH /manager/parkings/{id}/status` supports an empty body toggle, but explicit `{status}` is
  clearer for FE state management.
- Coordinates are percentages, not pixels.
- Upload flow is presign upload, browser PUT, then save `objectKey` with floor map patch.
- Kiosk list already includes `assignedStaffCount`.
- Staff device approval requires staff to be assigned to the selected kiosk first.
- Staff login and check-in depend on the approved device's kiosk context.

## Atomic Commit Suggestions

- `fix(manager-facility): enforce tenant scope for slot import and bulk status`
- `test(manager-facility): cover cross-tenant slot bulk status rejection`
- `docs(manager): add facility staff devices backend audit matrix`
- `docs(manager-devices): document kill switch and device history gaps`
