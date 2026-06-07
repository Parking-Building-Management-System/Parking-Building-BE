# Group 2 - Parking Facility & Daily Operations APIs

## 1. Group Purpose

This group explains how a parking building is configured and operated day to day. It connects manager setup APIs with staff kiosk workflows and the driver PWA map guide.

The key message for the teacher: the system turns facility configuration into real operational state transitions: slots become occupied, sessions become active, and exit releases resources.

## 2. Main Actors

- `PARKING_MANAGER`: configures facility, cards, staff, kiosks, and device approvals.
- `STAFF`: uses trusted kiosk device to check vehicles in and out.
- Driver/PWA: scans RFID card QR to view assigned slot and quote.

## 3. Endpoint Table

| Method | Path | Actor/Role | Purpose | Main Request DTO | Main Response DTO | DB tables touched |
|---|---|---|---|---|---|---|
| GET | `/manager/parkings` | `PARKING_MANAGER` | List tenant parkings | none | `List<ParkingResponse>` | `parkings` |
| POST | `/manager/parkings` | `PARKING_MANAGER` | Create parking | `ParkingRequest` | `ParkingResponse` | `parkings` |
| GET | `/manager/parkings/{id}` | `PARKING_MANAGER` | Get parking detail | path id | `ParkingResponse` | `parkings` |
| PUT | `/manager/parkings/{id}` | `PARKING_MANAGER` | Update parking metadata/status | `ParkingRequest` | `ParkingResponse` | `parkings` |
| PATCH | `/manager/parkings/{id}/status` | `PARKING_MANAGER` | Toggle parking status | `ParkingStatusRequest` | `ParkingStatusResponse` | `parkings` |
| GET | `/manager/parkings/{id}/topology` | `PARKING_MANAGER` | Return parking/floor/zone/slot tree | none | `ParkingTopologyResponse` | `parkings`, `floors`, `zones`, `slots` |
| GET | `/manager/parkings/{parkingId}/floors` | `PARKING_MANAGER` | List floors | none | `List<FloorResponse>` | `floors` |
| POST | `/manager/parkings/{parkingId}/floors` | `PARKING_MANAGER` | Create floor | `FloorRequest` | `FloorResponse` | `parkings`, `floors` |
| GET | `/manager/floors/{id}` | `PARKING_MANAGER` | Get floor | path id | `FloorResponse` | `floors` |
| PUT | `/manager/floors/{id}` | `PARKING_MANAGER` | Update floor | `FloorRequest` | `FloorResponse` | `floors` |
| DELETE | `/manager/floors/{id}` | `PARKING_MANAGER` | Soft-delete empty floor | path id | `ApiResponse<Void>` | `floors`, `zones` |
| GET | `/manager/floors/{floorId}/zones` | `PARKING_MANAGER` | List zones | none | `List<ZoneResponse>` | `zones` |
| POST | `/manager/floors/{floorId}/zones` | `PARKING_MANAGER` | Create zone | `ZoneRequest` | `ZoneResponse` | `parkings`, `floors`, `zones`, `vehicle_types` |
| GET | `/manager/zones/{id}` | `PARKING_MANAGER` | Get zone | path id | `ZoneResponse` | `zones` |
| PUT | `/manager/zones/{id}` | `PARKING_MANAGER` | Update zone | `ZoneRequest` | `ZoneResponse` | `zones`, `vehicle_types` |
| DELETE | `/manager/zones/{id}` | `PARKING_MANAGER` | Soft-delete empty zone | path id | `ApiResponse<Void>` | `zones`, `slots` |
| POST | `/manager/zones/{zoneId}/slots` | `PARKING_MANAGER` | Create slot | `SlotRequest` | `SlotResponse` | `slots`, `zones`, `parkings`, `floors` |
| GET | `/manager/slots` | `PARKING_MANAGER` | Search slots | filters, page, size | `PageResponse<SlotResponse>` | `slots`, `zones`, `floors`, `parkings` |
| GET | `/manager/slots/{id}` | `PARKING_MANAGER` | Get slot detail | path id | `SlotResponse` | `slots` |
| PUT | `/manager/slots/{id}` | `PARKING_MANAGER` | Update slot metadata/status | `SlotRequest` | `SlotResponse` | `slots` |
| DELETE | `/manager/slots/{id}` | `PARKING_MANAGER` | Soft-delete slot | path id | `ApiResponse<Void>` | `slots` |
| PATCH | `/manager/slots/{id}/status` | `PARKING_MANAGER` | Update one slot status | `SlotStatusRequest` | `SlotResponse` | `slots` |
| PATCH | `/manager/slots/bulk-status` | `PARKING_MANAGER` | Update many slot statuses | `SlotBulkStatusRequest` | `SlotBulkStatusResponse` | `slots` |
| POST | `/manager/slots/import` | `PARKING_MANAGER` | Import slots from Excel | multipart file | `SlotImportResponse` | `slots`, `zones`, `floors`, `parkings` |
| GET | `/manager/slots/export` | `PARKING_MANAGER` | Export slots to Excel | none | binary Excel | `slots`, `zones`, `floors`, `parkings` |
| PATCH | `/manager/floors/{id}/map` | `PARKING_MANAGER` | Attach/update floor map image | `FloorMapRequest` | `FloorMapResponse` | `floors` |
| GET | `/manager/floors/{id}/map` | `PARKING_MANAGER` | Get map with slot pins | path id | `FloorMapDetailResponse` | `floors`, `slots` |
| PATCH | `/manager/slots/{id}/coordinate` | `PARKING_MANAGER` | Update slot pin | `SlotCoordinateRequest` | `SlotCoordinateResponse` | `slots` |
| PATCH | `/manager/slots/coordinates` | `PARKING_MANAGER` | Bulk update slot pins | `SlotCoordinateBulkRequest` | `SlotCoordinateBulkResponse` | `slots` |
| POST | `/manager/storage/presign-upload` | `PARKING_MANAGER` | Generate upload URL for map/object | `PresignUploadRequest` | `PresignUploadResponse` | storage only |
| GET | `/manager/storage/presign-download` | `PARKING_MANAGER` | Generate download URL | `objectKey` | `PresignDownloadResponse` | storage only |
| GET | `/manager/rfid-cards` | `PARKING_MANAGER` | List RFID pool | filters, page, size | `PageResponse<RfidCardResponse>` | `rfid_cards` |
| POST | `/manager/rfid-cards/generate` | `PARKING_MANAGER` | Generate cards and QR tokens | `RfidCardGenerateRequest` | `RfidCardGenerateResponse` | `rfid_cards` |
| PATCH | `/manager/rfid-cards/{id}/status` | `PARKING_MANAGER` | Activate/suspend card | `RfidCardStatusRequest` | `RfidCardResponse` | `rfid_cards` |
| GET | `/manager/staff` | `PARKING_MANAGER` | List staff accounts | filters, page, size | `PageResponse<ManagerStaffResponse>` | `users`, `user_roles` |
| POST | `/manager/staff` | `PARKING_MANAGER` | Create staff user | `ManagerStaffCreateRequest` | `ManagerStaffResponse` | `users`, `roles`, `user_roles` |
| GET | `/manager/staff/{id}` | `PARKING_MANAGER` | Get staff detail | path id | `ManagerStaffResponse` | `users` |
| PUT | `/manager/staff/{id}` | `PARKING_MANAGER` | Update staff info | `ManagerStaffUpdateRequest` | `ManagerStaffResponse` | `users` |
| PATCH | `/manager/staff/{id}/status` | `PARKING_MANAGER` | Activate/deactivate staff | `ManagerStaffStatusRequest` | `ManagerStaffResponse` | `users` |
| POST | `/manager/staff/{id}/reset-password` | `PARKING_MANAGER` | Reset staff password | `ManagerStaffPasswordResetRequest` | `ManagerStaffResponse` | `users` |
| GET | `/manager/kiosks` | `PARKING_MANAGER` | List kiosks/gates | filters | `List<ManagerKioskResponse>` | `kiosk` |
| POST | `/manager/kiosks` | `PARKING_MANAGER` | Create entry/exit/mixed kiosk | `ManagerKioskRequest` | `ManagerKioskResponse` | `kiosk`, `parkings` |
| GET | `/manager/kiosks/{id}` | `PARKING_MANAGER` | Get kiosk | path id | `ManagerKioskResponse` | `kiosk` |
| PUT | `/manager/kiosks/{id}` | `PARKING_MANAGER` | Update kiosk | `ManagerKioskUpdateRequest` | `ManagerKioskResponse` | `kiosk` |
| PATCH | `/manager/kiosks/{id}/status` | `PARKING_MANAGER` | Activate/inactivate kiosk | `ManagerKioskStatusRequest` | `ManagerKioskResponse` | `kiosk` |
| DELETE | `/manager/kiosks/{id}` | `PARKING_MANAGER` | Soft-delete kiosk | path id | `ApiResponse<Void>` | `kiosk` |
| GET | `/manager/kiosks/{id}/staff` | `PARKING_MANAGER` | List staff assigned to kiosk | path id | `List<ManagerKioskStaffResponse>` | `kiosk_staff`, `users` |
| POST | `/manager/kiosks/{id}/staff/{staffId}` | `PARKING_MANAGER` | Assign staff to kiosk | path ids | `ManagerKioskStaffResponse` | `kiosk_staff`, `users`, `kiosk` |
| DELETE | `/manager/kiosks/{id}/staff/{staffId}` | `PARKING_MANAGER` | Unassign staff | path ids | `ApiResponse<Void>` | `kiosk_staff` |
| GET | `/manager/device-approvals` | `PARKING_MANAGER` | List pending staff devices | none | `List<ManagerDeviceResponse>` | `devices` |
| POST | `/manager/device-approvals/{id}/approve` | `PARKING_MANAGER` | Approve staff device and bind kiosk | `DeviceApprovalRequest` | `ManagerDeviceResponse` | `devices`, `kiosk` |
| POST | `/manager/device-approvals/{id}/reject` | `PARKING_MANAGER` | Reject pending device | path id | `ManagerDeviceResponse` | `devices` |
| POST | `/manager/devices/{id}/revoke` | `PARKING_MANAGER` | Revoke trusted staff device | path id | `ManagerDeviceResponse` | `devices` |
| GET | `/staff/rfid-cards/available` | `STAFF` | Find free cards for check-in | search, limit | `List<AvailableRfidCardResponse>` | `rfid_cards`, `parking_sessions` |
| POST | `/staff/parking-sessions/check-in` | `STAFF` entry kiosk | Create active session and assign slot | `ParkingSessionCheckInRequest` | `ParkingSessionCheckInResponse` | `parking_sessions`, `slots`, `rfid_cards`, `vehicle_types` |
| GET | `/pwa/cards/{qrToken}/active-session` | Driver/PWA | Show active session map and assigned slot | QR token | `CardActiveSessionResponse` | `rfid_cards`, `parking_sessions`, `slots`, `floors`, `zones` |
| POST | `/staff/parking-sessions/exit-preview` | `STAFF` exit kiosk | Calculate exit decision | `ExitPreviewRequest` | `ExitPreviewResponse` | `parking_sessions`, `rfid_cards`, `pricing_rules` |
| POST | `/staff/parking-sessions/complete-exit` | `STAFF` exit kiosk | Complete session and release slot | `CompleteExitRequest` | `CompleteExitResponse` | `parking_sessions`, `slots`, `rfid_cards` |

## 4. Typical Demo Sequence

1. Manager logs in and opens facility topology.
2. Manager shows floors/zones/slots and map pins.
3. Manager shows RFID card pool.
4. Manager shows staff, kiosks, and assignments.
5. Staff tries login from kiosk device. If blocked, manager approves device.
6. Staff entry lists available cards and checks in a car.
7. Response returns assigned slot and QR token.
8. Driver PWA opens active session by QR token.
9. Staff exit previews fee/decision and completes exit.

## 5. Important Validations and Business Rules

- All manager data is tenant-scoped through JWT tenant context.
- Parking, floor, zone, and slot codes must be unique in their scope.
- Floors with zones and zones with slots cannot be deleted.
- Slot import validates headers, duplicate slot codes, and zone/floor/parking references.
- Slot coordinate values are percentage pins and must be valid map coordinates.
- RFID card must be `ACTIVE` and not linked to another `ACTIVE` session before check-in.
- Staff APIs require a trusted device and staff-to-kiosk assignment.
- Entry APIs require entry-capable kiosk context.
- Exit APIs require `EXIT` or `ENTRY_EXIT` kiosk context.
- Check-in sets `parking_sessions.status=ACTIVE` and `slots.status=OCCUPIED`.
- Complete exit sets `parking_sessions.status=COMPLETED` and `slots.status=AVAILABLE`.

## 6. Common Error Codes

- `UNAUTHENTICATED`: missing/expired staff or manager token.
- `FORBIDDEN_ACTION`: wrong role, inactive tenant, missing kiosk context, wrong kiosk type.
- `DEVICE_NOT_TRUST`: staff device pending/rejected/not bound to kiosk.
- `RESOURCE_NOT_FOUND`: parking/floor/zone/slot/card/session not found.
- `DUPLICATE_RESOURCE`: duplicate facility code or card already in use.
- `INVALID_INPUT`: invalid coordinates, status transition, Excel file, payment mode, or inactive vehicle type.

## 7. How to Test With Bruno

Use folders:

- `02 Manager Facility`
- `03 Manager Staff Devices`
- `04 Staff Entry`
- `05 PWA Driver`
- `08 Staff Exit Gate`
- `09 Smoke Flows`

Important variables:

- `managerToken`, `staffEntryToken`, `staffExitToken`
- `parkingId`, `floorId`, `zoneId`, `slotId`
- `staffId`, `kioskId`, `exitKioskId`
- `deviceApprovalId`, `deviceId`
- `cardCode`, `qrToken`, `sessionId`

Run order:

1. Manager facility setup requests.
2. Manager staff/kiosk/device setup requests.
3. Staff entry login and `Check In Parking Session`.
4. Copy `qrToken` and `sessionId`.
5. Driver PWA active session and checkout quote.
6. Staff exit preview and complete exit.

## 8. Presentation Notes

- Explain the dependency chain: facility setup -> staff/kiosk trust -> entry session -> PWA guide -> exit release.
- Emphasize state changes instead of listing CRUD: slot `AVAILABLE -> OCCUPIED -> AVAILABLE`, session `ACTIVE -> COMPLETED`.
- Show the QR token as an access handle for the driver, not a JWT.
- Mention that staff devices are controlled so a random browser cannot operate a gate.
