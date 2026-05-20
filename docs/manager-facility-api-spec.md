# PARKING_MANAGER Facility API Specification

This document is the frontend contract for the Facility Management flow.

Base paths:

```text
/manager/parkings
/manager/floors
/manager/zones
/manager/slots
```

All JSON endpoints return the standard envelope:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {},
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/..."
}
```

`GET /manager/slots/export` is the only exception: it returns a binary Excel file, not the JSON
envelope.

## Security And Tenant Isolation

All endpoints require:

```http
Authorization: Bearer <access_token>
```

Required Spring Security role:

```text
ROLE_PARKING_MANAGER
```

The frontend must not send a tenant id. The backend extracts the exact JWT claim named
`tenant_id`, sets `TenantContext`, and relies on the Hibernate tenant filter for all tenant-scoped
queries.

Common errors:

| HTTP | Code | Meaning |
| --- | ---: | --- |
| 400 | `4000` | Bean validation failed |
| 400 | `4001` | Malformed JSON |
| 400 | `4002` | Business validation failed |
| 401 | `4010` | Missing, invalid, expired, revoked token, or missing `tenant_id` claim |
| 403 | `4030` | Authenticated user is not `PARKING_MANAGER` |
| 404 | `4040` | Tenant-scoped resource not found |
| 409 | `4090` | Duplicate code or database uniqueness conflict |
| 500 | `5000` | Unexpected server error |

## Field Naming Rules

Use these exact JSON field names. Do not rename them in the frontend model layer.

| DTO | Exact fields |
| --- | --- |
| `ParkingResponse` | `id`, `code`, `name`, `address`, `totalCapacity`, `status` |
| `ParkingStatusResponse` | `id`, `status` |
| `ParkingTopologyResponse` | `id`, `code`, `name`, `status`, `totalCapacity`, `floors` |
| `FloorTopologyResponse` | `id`, `code`, `name`, `displayOrder`, `zones` |
| `ZoneTopologyResponse` | `id`, `code`, `name`, `vehicleTypeCode`, `vehicleTypeName`, `capacity`, `slotCount`, `status` |
| `FloorRequest` | `code`, `name`, `displayOrder`, `active` |
| `FloorResponse` | `id`, `parkingId`, `code`, `name`, `displayOrder`, `active` |
| `ZoneRequest` | `code`, `name`, `vehicleTypeCode`, `capacity`, `status` |
| `ZoneResponse` | `id`, `parkingId`, `floorId`, `code`, `name`, `vehicleTypeCode`, `vehicleTypeName`, `capacity`, `status` |
| `SlotResponse` | `id`, `parkingId`, `parkingName`, `floorId`, `floorName`, `zoneId`, `zoneName`, `code`, `slotNumber`, `status` |
| `SlotBulkStatusRequest` | `slotIds`, `newStatus` |
| `SlotBulkStatusResponse` | `updatedCount`, `newStatus` |
| `SlotImportResponse` | `insertedCount` |

There is no field named `tenantName`, `parkingCode`, `zoneCode`, or `slotCode` in
`SlotResponse`. The slot code field in API responses is named `code`. The query parameter for slot
search is named `slotCode`.

Enum values:

```text
ParkingStatus: ACTIVE, INACTIVE, MAINTENANCE
ZoneStatus: ACTIVE, INACTIVE, MAINTENANCE
SlotStatus: AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE, LOCKED
```

Bulk slot status accepts only:

```text
AVAILABLE, MAINTENANCE, LOCKED
```

## Parkings

### List Parkings

```http
GET /manager/parkings
```

Query parameters: none.

Request body: none.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
      "code": "VINCOM-DK",
      "name": "Vincom Dong Khoi",
      "address": "72 Le Thanh Ton, District 1, Ho Chi Minh City",
      "totalCapacity": 186,
      "status": "ACTIVE"
    },
    {
      "id": "0f4ab313-c8c5-3168-b958-060746b4e815",
      "code": "VINCOM-TD",
      "name": "Vincom Thao Dien",
      "address": "161 Xa Lo Ha Noi, Thao Dien, Thu Duc City",
      "totalCapacity": 168,
      "status": "MAINTENANCE"
    }
  ],
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/parkings"
}
```

Field notes:

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | UUID string | Parking id |
| `code` | string | Parking/building code |
| `name` | string | Parking/building display name |
| `address` | string or null | Full address |
| `totalCapacity` | number | Live count of non-deleted slots under the parking |
| `status` | enum | `ParkingStatus` |

### Toggle Parking Status

```http
PATCH /manager/parkings/{id}/status
```

Path parameters:

| Name | Type | Required |
| --- | --- | --- |
| `id` | UUID | Yes |

Request body: none.

Behavior:

- If current `status` is `ACTIVE`, backend changes it to `MAINTENANCE`.
- If current `status` is not `ACTIVE`, backend changes it to `ACTIVE`.
- Topology cache for this parking is evicted.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
    "status": "MAINTENANCE"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/parkings/8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e/status"
}
```

## Parking Topology

### Get Parking Topology

```http
GET /manager/parkings/{id}/topology
```

Path parameters:

| Name | Type | Required |
| --- | --- | --- |
| `id` | UUID | Yes |

Query parameters: none.

Request body: none.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "id": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
    "code": "VINCOM-DK",
    "name": "Vincom Dong Khoi",
    "status": "ACTIVE",
    "totalCapacity": 186,
    "floors": [
      {
        "id": "0f1b7254-5c66-33d7-af32-1dd0fdca1307",
        "code": "B1",
        "name": "Basement 1",
        "displayOrder": 1,
        "zones": [
          {
            "id": "e9244327-5929-316f-9b8c-42f9d8fb4e47",
            "code": "B1-A",
            "name": "B1 Zone A - Premium Cars",
            "vehicleTypeCode": "CAR",
            "vehicleTypeName": "Car",
            "capacity": 20,
            "slotCount": 20,
            "status": "ACTIVE"
          }
        ]
      }
    ]
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/parkings/8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e/topology"
}
```

Topology field notes:

| Field | Type | Meaning |
| --- | --- | --- |
| `result.id` | UUID string | Parking id |
| `result.code` | string | Parking code |
| `result.name` | string | Parking display name |
| `result.status` | enum | `ParkingStatus` |
| `result.totalCapacity` | number | Live slot count for parking |
| `result.floors` | array | Array of `FloorTopologyResponse` |
| `floors[].id` | UUID string | Floor id |
| `floors[].code` | string | Floor code, for example `B1`, `B2`, `G`, `L1` |
| `floors[].name` | string | Floor display name |
| `floors[].displayOrder` | number | Sort order for UI tree |
| `floors[].zones` | array | Array of `ZoneTopologyResponse` |
| `zones[].id` | UUID string | Zone id |
| `zones[].code` | string | Zone code, for example `B1-A` |
| `zones[].name` | string | Zone display name |
| `zones[].vehicleTypeCode` | string or null | Global vehicle type code |
| `zones[].vehicleTypeName` | string or null | Global vehicle type display name |
| `zones[].capacity` | number | Configured zone capacity |
| `zones[].slotCount` | number | Live count of non-deleted slots in the zone |
| `zones[].status` | enum | `ZoneStatus` |

Cache behavior:

| Item | Value |
| --- | --- |
| Redis key | `smartpark:tenant:{tenantId}:topology:{parkingId}` |
| TTL | 10 minutes |
| Cache strategy | Cache-aside |
| Evicted by | Parking status update, floor create/update/delete, zone create/update/delete, slot import, slot bulk status update |

## Floors

### List Floors

```http
GET /manager/parkings/{parkingId}/floors
```

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "0f1b7254-5c66-33d7-af32-1dd0fdca1307",
      "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
      "code": "B1",
      "name": "Basement 1",
      "displayOrder": 1,
      "active": true
    }
  ],
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/parkings/8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e/floors"
}
```

### Create Floor

```http
POST /manager/parkings/{parkingId}/floors
Content-Type: application/json
```

Request body:

```json
{
  "code": "B4",
  "name": "Basement 4",
  "displayOrder": 4,
  "active": true
}
```

Request fields:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `code` | string | Yes | Non-blank, max 50, unique in parking |
| `name` | string | Yes | Non-blank, max 100 |
| `displayOrder` | number | Yes | Not null |
| `active` | boolean | No | Defaults to `true` when omitted |

Success response `result` is exactly `FloorResponse`.

### Get Floor

```http
GET /manager/floors/{id}
```

Success response `result` is exactly `FloorResponse`.

### Update Floor

```http
PUT /manager/floors/{id}
Content-Type: application/json
```

Request body is exactly `FloorRequest`.

Success response `result` is exactly `FloorResponse`.

### Delete Floor

```http
DELETE /manager/floors/{id}
```

Request body: none.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/floors/0f1b7254-5c66-33d7-af32-1dd0fdca1307"
}
```

Delete fails with `4002` if the floor still has zones.

## Zones

### List Zones

```http
GET /manager/floors/{floorId}/zones
```

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "e9244327-5929-316f-9b8c-42f9d8fb4e47",
      "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
      "floorId": "0f1b7254-5c66-33d7-af32-1dd0fdca1307",
      "code": "B1-A",
      "name": "B1 Zone A - Premium Cars",
      "vehicleTypeCode": "CAR",
      "vehicleTypeName": "Car",
      "capacity": 20,
      "status": "ACTIVE"
    }
  ],
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/floors/0f1b7254-5c66-33d7-af32-1dd0fdca1307/zones"
}
```

### Create Zone

```http
POST /manager/floors/{floorId}/zones
Content-Type: application/json
```

Request body:

```json
{
  "code": "B4-E",
  "name": "B4 Zone E - EV Chargers",
  "vehicleTypeCode": "ELECTRIC_CAR",
  "capacity": 12,
  "status": "ACTIVE"
}
```

Request fields:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `code` | string | Yes | Non-blank, max 50, unique in parking |
| `name` | string | Yes | Non-blank, max 255 |
| `vehicleTypeCode` | string | Yes | Non-blank, max 50, must exist in global `vehicle_types`, must be active |
| `capacity` | number | Yes | Not null, `>= 0` |
| `status` | enum or null | No | `ZoneStatus`; defaults to `ACTIVE` when omitted |

Success response `result` is exactly `ZoneResponse`.

### Get Zone

```http
GET /manager/zones/{id}
```

Success response `result` is exactly `ZoneResponse`.

### Update Zone

```http
PUT /manager/zones/{id}
Content-Type: application/json
```

Request body is exactly `ZoneRequest`.

Success response `result` is exactly `ZoneResponse`.

### Delete Zone

```http
DELETE /manager/zones/{id}
```

Request body: none.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/zones/e9244327-5929-316f-9b8c-42f9d8fb4e47"
}
```

Delete fails with `4002` if the zone still has slots.

## Slots

### Search Slots

```http
GET /manager/slots
```

Query parameters:

| Param | Type | Required | Default | Exact backend name | Meaning |
| --- | --- | --- | --- | --- | --- |
| `zoneId` | UUID | No | none | `zoneId` | Filters slots to one zone id |
| `status` | enum | No | none | `status` | Filters by `SlotStatus` |
| `slotCode` | string | No | none | `slotCode` | Searches against `Slot.code` |
| `exact` | boolean | No | `false` | `exact` | If `true`, exact case-insensitive code match; otherwise LIKE search |
| `page` | number | No | `0` | `page` | 0-based page index |
| `size` | number | No | `20` | `size` | Page size, valid range `1..100` |

Examples:

```http
GET /manager/slots?page=0&size=20
GET /manager/slots?zoneId=e9244327-5929-316f-9b8c-42f9d8fb4e47&status=AVAILABLE
GET /manager/slots?slotCode=A-&exact=false&page=0&size=50
GET /manager/slots?slotCode=A-01&exact=true
```

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "content": [
      {
        "id": "11871a23-6075-3bb2-b145-a05950fba95c",
        "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
        "parkingName": "Vincom Dong Khoi",
        "floorId": "0f1b7254-5c66-33d7-af32-1dd0fdca1307",
        "floorName": "Basement 1",
        "zoneId": "e9244327-5929-316f-9b8c-42f9d8fb4e47",
        "zoneName": "B1 Zone A - Premium Cars",
        "code": "A-01",
        "slotNumber": "B1-A-01",
        "status": "AVAILABLE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/slots"
}
```

`SlotResponse` field notes:

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | UUID string | Slot id |
| `parkingId` | UUID string | Parent parking id |
| `parkingName` | string | Parent parking display name |
| `floorId` | UUID string or null | Parent floor id |
| `floorName` | string or null | Parent floor display name |
| `zoneId` | UUID string | Parent zone id |
| `zoneName` | string | Parent zone display name |
| `code` | string | Slot code. This is not named `slotCode` in the response |
| `slotNumber` | string | Human-facing slot number/label |
| `status` | enum | `SlotStatus` |

### Bulk Update Slot Status

```http
PATCH /manager/slots/bulk-status
Content-Type: application/json
```

Request body:

```json
{
  "slotIds": [
    "11871a23-6075-3bb2-b145-a05950fba95c",
    "ac3bc1b3-dad4-3950-8097-c3899a86ae8a"
  ],
  "newStatus": "MAINTENANCE"
}
```

Request fields:

| Field | Type | Required | Validation |
| --- | --- | --- | --- |
| `slotIds` | array of UUID strings | Yes | Must not be empty |
| `newStatus` | enum | Yes | Must be `AVAILABLE`, `MAINTENANCE`, or `LOCKED` |

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "updatedCount": 2,
    "newStatus": "MAINTENANCE"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/slots/bulk-status"
}
```

Behavior:

- The update runs in one transaction.
- If any requested slot id is missing under the JWT tenant, the call fails with `4040`.
- Topology cache is evicted for every affected parking.
- The AI allocation trigger is intentionally not called.

### Import Slots From Excel

```http
POST /manager/slots/import
Content-Type: multipart/form-data
```

Form fields:

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `file` | Excel file | Yes | Workbook parsed from the first sheet |

Required Excel headers:

| Header | Required | Meaning |
| --- | --- | --- |
| `parkingCode` | Yes | Matches `Parking.code` under the JWT tenant |
| `floorCode` | Yes | Matches `Floor.code` under the resolved parking |
| `zoneCode` | Yes | Matches `Zone.code` under the resolved floor |
| `slotCode` | Yes | New slot code to create |

Optional Excel headers:

| Header | Required | Meaning |
| --- | --- | --- |
| `slotNumber` | No | Human-facing slot label. Defaults to `slotCode` when blank |
| `status` | No | `SlotStatus`. Defaults to `AVAILABLE` when blank |

Header matching ignores punctuation and case. For example, `Slot Code`, `slot_code`, and `slotCode`
all map to `slotCode`.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "insertedCount": 25
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/manager/slots/import"
}
```

Import rules:

- Blank rows are skipped.
- Parking, floor, and zone must exist under the authenticated tenant.
- `slotCode` must be unique inside the target zone.
- Duplicate slot codes inside the uploaded workbook are rejected.
- If any row is invalid, the transaction rolls back and no slots from that upload are inserted.
- Topology cache is evicted for every parking affected by the import.

### Export Slots To Excel

```http
GET /manager/slots/export
```

Request body: none.

Response headers:

```http
Content-Type: application/vnd.ms-excel
Content-Disposition: attachment; filename="smartpark-slots-2026-05-20.xlsx"
```

Response body:

```text
Binary Excel workbook
```

The Excel sheet is named `slots` and contains these columns:

| Column | Source field |
| --- | --- |
| `Parking Code` | `slot.parking.code` |
| `Parking Name` | `slot.parking.name` |
| `Floor Code` | `slot.floor.code`, blank when null |
| `Floor Name` | `slot.floor.name`, blank when null |
| `Zone Code` | `slot.zone.code` |
| `Zone Name` | `slot.zone.name` |
| `Slot Code` | `slot.code` |
| `Slot Number` | `slot.slotNumber` |
| `Status` | `slot.status` |

The export is tenant-scoped through the JWT `tenant_id` claim.
