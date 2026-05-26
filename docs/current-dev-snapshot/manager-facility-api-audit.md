# Manager Facility API Audit

## Existing APIs

| Domain | Method | Path | Status | Files | Notes |
|---|---|---|---|---|---|
| Parking | GET | `/manager/parkings` | WORKING | `ManagerFacilityController`, `ManagerFacilityServiceImpl` | Tenant-scoped list with live slot capacity. |
| Parking | POST | `/manager/parkings` | WORKING | same | Added for tenant self-setup. |
| Parking | GET | `/manager/parkings/{id}` | WORKING | same | Added tenant-scoped detail. |
| Parking | PUT | `/manager/parkings/{id}` | WORKING | same | Added tenant-scoped update. |
| Parking | PATCH | `/manager/parkings/{id}/status` | WORKING | same | Optional explicit status body; no body keeps existing toggle behavior. |
| Parking | GET | `/manager/parkings/{id}/topology` | WORKING | same | Cached topology by tenant and parking. |
| Floor | GET | `/manager/parkings/{parkingId}/floors` | WORKING | `ManagerFacilityController`, `ManagerFacilityServiceImpl` | Tenant parking is resolved before list. |
| Floor | POST | `/manager/parkings/{parkingId}/floors` | WORKING | same | Creates floor under current tenant parking. |
| Floor | GET | `/manager/floors/{id}` | WORKING | same | Tenant-scoped detail. |
| Floor | PUT | `/manager/floors/{id}` | WORKING | same | Tenant-scoped update. |
| Floor | DELETE | `/manager/floors/{id}` | WORKING | same | Soft delete only if empty. |
| Zone | GET | `/manager/floors/{floorId}/zones` | WORKING | `ManagerFacilityController`, `ManagerFacilityServiceImpl` | Tenant floor is resolved before list. |
| Zone | POST | `/manager/floors/{floorId}/zones` | WORKING | same | Validates global vehicle type. |
| Zone | GET | `/manager/zones/{id}` | WORKING | same | Tenant-scoped detail. |
| Zone | PUT | `/manager/zones/{id}` | WORKING | same | Tenant-scoped update. |
| Zone | DELETE | `/manager/zones/{id}` | WORKING | same | Soft delete only if empty. |
| Slot | GET | `/manager/slots` | WORKING | `ManagerSlotController`, `ManagerSlotServiceImpl` | Paged tenant slot search. |
| Slot | POST | `/manager/zones/{zoneId}/slots` | WORKING | `ManagerSlotManagementController`, `ManagerSlotServiceImpl` | Added single-slot create. |
| Slot | GET | `/manager/slots/{id}` | WORKING | same | Added tenant-scoped detail. |
| Slot | PUT | `/manager/slots/{id}` | WORKING | same | Added tenant-scoped update. |
| Slot | DELETE | `/manager/slots/{id}` | WORKING | same | Added soft delete. |
| Slot | PATCH | `/manager/slots/{id}/status` | WORKING | same | Added single status update. |
| Slot | POST | `/manager/slots/import` | WORKING | `ManagerSlotController`, `ManagerSlotServiceImpl` | Excel import. |
| Slot | GET | `/manager/slots/export` | WORKING | same | Excel export now explicitly filters current tenant. |
| Slot | PATCH | `/manager/slots/bulk-status` | WORKING | same | Tenant-constrained bulk update. |
| RFID Card | GET | `/manager/rfid-cards` | WORKING | `ManagerRfidCardController`, `ManagerRfidCardServiceImpl` | Added paged card list. |
| RFID Card | POST | `/manager/rfid-cards/generate` | WORKING | same | Added idempotent tenant card pool generation. |
| RFID Card | PATCH | `/manager/rfid-cards/{id}/status` | WORKING | same | Added tenant-scoped status update. |

## CRUD Matrix

| Domain | Create | List | Detail | Update | Delete/Status | Import/Generate | Notes |
|---|---|---|---|---|---|---|---|
| Parking | WORKING | WORKING | WORKING | WORKING | WORKING | MISSING | Delete is intentionally absent for MVP. |
| Floor | WORKING | WORKING | WORKING | WORKING | WORKING | MISSING | Delete is soft delete and requires no zones. |
| Zone | WORKING | WORKING | WORKING | WORKING | WORKING | MISSING | Delete is soft delete and requires no slots. |
| Slot | WORKING | WORKING | WORKING | WORKING | WORKING | WORKING | Import/export and bulk status are available. |
| RFID Card | MISSING | WORKING | MISSING | MISSING | WORKING | WORKING | MVP supports list, generate, status only. |

## Tenant Isolation Check

Parking:

- `tenantId` comes from authenticated manager JWT via `ManagerTenantContext`.
- Create uses `currentTenantReference()`.
- Detail/update/status use `findByIdAndTenantIdAndIsDeletedFalse`.
- List uses `findAllByTenantIdAndIsDeletedFalseOrderByNameAsc`.

Floor:

- Client sends only parking/floor ids, not tenant id.
- Parent parking is tenant-scoped before floor list/create.
- Detail/update/delete use `findByIdAndTenantIdAndDeletedFalse`.

Zone:

- Client sends only floor/zone ids, not tenant id.
- Parent floor is tenant-scoped before zone list/create.
- Detail/update/delete use `findByIdAndTenantIdAndIsDeletedFalse`.

Slot:

- Client sends only zone/slot ids, not tenant id.
- Create resolves tenant-scoped zone and copies parking/floor from that zone.
- Detail/update/delete/status use `findByIdAndTenantIdAndIsDeletedFalse`.
- Bulk status updates include `tenantId` in the update query.
- Export passes current tenant id explicitly.

RFID Card:

- Cards are tenant-owned, not parking/slot-owned.
- List/generate/status use current tenant id only.
- Card code uniqueness is checked within tenant.
