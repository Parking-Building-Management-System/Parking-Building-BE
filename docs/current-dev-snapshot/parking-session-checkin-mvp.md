# Parking Session Check-in MVP

## Endpoint Contract

```http
POST /staff/parking-sessions/check-in
Authorization: Bearer <staff-access-token>
Content-Type: application/json
```

Role: `STAFF`.

Tenant source: authenticated JWT claim `tenant_id`. The request does not accept `tenantId`.

## Request Sample

```json
{
  "plateNumber": "51A-12345",
  "cardCode": "VIN-RFID-001",
  "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
  "entryImageUrl": "https://example.com/entry/51A-12345.jpg"
}
```

Optional field:

```json
{
  "vehicleTypeId": "2b1bbf30-65cd-327b-a4a5-4d49ee80b2a6"
}
```

If `vehicleTypeId` is omitted, backend uses the selected slot's zone vehicle type. If that zone has no vehicle type, request fails with `4002`.

## Response Sample

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "sessionId": "019e62c7-3e00-7b0b-9d7e-3bb58f9b4b6a",
    "plateNumber": "51A-12345",
    "cardCode": "VIN-RFID-001",
    "assignedSlotId": "d2c2a1a6-1f2b-3b52-9d24-8f0c4ecf51a0",
    "assignedSlotCode": "A-01",
    "zoneId": "0f8104d3-99ed-312d-9d9f-6c94803b29c4",
    "zoneName": "B1 Zone A - Premium Cars",
    "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
    "entryTime": "2026-05-26T14:30:00",
    "status": "ACTIVE"
  },
  "timestamp": "2026-05-26T07:30:00Z",
  "path": "/staff/parking-sessions/check-in"
}
```

## DB Tables Touched

- `parking_sessions`: insert new ACTIVE session.
- `slots`: selected slot status changes from `AVAILABLE` to `OCCUPIED`.
- `rfid_cards`: read only in current enum model.
- `parkings`, `zones`, `vehicle_types`, `tenants`: read/relations.

## Assumptions Made

- Current schema uses `parking_sessions.slot_id` as the assigned slot. There is no `actual_slot_id` column in this repo.
- Current schema uses `parking_sessions.check_in_at` as entry time.
- Current `SlotStatus` has no `ASSIGNED`; MVP uses `OCCUPIED`.
- Current `RfidCardStatus` has no `IN_USE`; card reuse is blocked by checking for an ACTIVE parking session using the same card.
- MVP uses `parkingId` because the current request model and existing repositories already support parking-level facility operations. Kiosk enforcement is not implemented in this task.
- The deterministic slot algorithm picks the first `AVAILABLE` slot under the parking ordered by zone name, zone code, then slot code.
