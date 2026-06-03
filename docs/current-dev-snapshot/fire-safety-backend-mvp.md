# Fire Safety Backend MVP

## Domain Model

Fire extinguishers are tenant-scoped assets pinned to the existing facility hierarchy:

- `fire_extinguishers`: tenant, parking, floor, optional zone, code, type, status, location, percent map coordinates, manufacture/expiry dates, inspection timestamps, note, soft delete.
- `fire_extinguisher_inspections`: tenant, extinguisher, optional inspecting user, inspection result, checklist booleans, optional legacy photo URL, optional storage photo object key, note, inspected timestamp, next inspection timestamp.

Enums:

- Type: `CO2`, `POWDER`, `FOAM`, `WATER`, `OTHER`
- Status: `ACTIVE`, `EXPIRED`, `MISSING`, `DAMAGED`, `MAINTENANCE`, `REPLACED`
- Inspection result: `OK`, `NEEDS_REPLACEMENT`, `DAMAGED`, `MISSING`, `EXPIRED`

Coordinates use the same map convention as slots: `0..100` percent from the floor map image.

## Manager APIs

All manager APIs require `PARKING_MANAGER`; tenant comes from JWT.

### Fire Extinguishers

`GET /manager/fire-extinguishers`

Filters: `parkingId`, `floorId`, `zoneId`, `status`, `type`, `search`, `expiringWithinDays`, `page`, `size`.

Search behavior:

- Omitted, `null`, and blank `search` are treated as no search and return the normal filtered list.
- Nonblank `search` is trimmed and matched case-insensitively against `code` and `locationDescription`.
- Production fix: the list path no longer sends nullable search into the PostgreSQL `lower/like` expression, avoiding `function lower(bytea) does not exist` when search is empty.

`POST /manager/fire-extinguishers`

```json
{
  "parkingId": "parking-id",
  "floorId": "floor-id",
  "zoneId": "zone-id",
  "code": "FE-B1-001",
  "type": "CO2",
  "locationDescription": "Near elevator B1",
  "xCoordinate": 64.25,
  "yCoordinate": 41.75,
  "manufactureDate": "2025-01-01",
  "expiryDate": "2027-01-01",
  "nextInspectionAt": "2026-07-01T00:00:00",
  "status": "ACTIVE",
  "note": "Demo extinguisher"
}
```

Other endpoints:

- `GET /manager/fire-extinguishers/{id}`
- `PUT /manager/fire-extinguishers/{id}`
- `PATCH /manager/fire-extinguishers/{id}/status`
- `PATCH /manager/fire-extinguishers/{id}/coordinate`
- `DELETE /manager/fire-extinguishers/{id}` soft deletes
- `GET /manager/fire-extinguishers/summary`

Summary response:

```json
{
  "total": 40,
  "active": 32,
  "expired": 3,
  "missing": 1,
  "damaged": 2,
  "maintenance": 2,
  "dueInspection": 6,
  "expiringSoon": 5
}
```

### Fire Safety Map

`GET /manager/floors/{floorId}/fire-safety-map`

Returns floor map metadata and extinguisher pins. It reuses `floors.map_image_url`; no upload logic is duplicated.

```json
{
  "parkingId": "parking-id",
  "parkingName": "Bcons Plaza",
  "floorId": "floor-id",
  "floorName": "Basement 1",
  "floorCode": "B1",
  "mapImageUrl": "tenants/.../floor-maps/demo/...",
  "coordinateMode": "PERCENT",
  "extinguishers": [
    {
      "id": "extinguisher-id",
      "code": "FE-B1-001",
      "type": "CO2",
      "status": "ACTIVE",
      "zoneId": "zone-id",
      "zoneName": "B1 Resident Cars",
      "locationDescription": "Near elevator",
      "xCoordinate": 22.5,
      "yCoordinate": 34.8,
      "expiryDate": "2027-01-01",
      "nextInspectionAt": "2026-07-01T00:00:00",
      "hasCoordinate": true
    }
  ]
}
```

### Inspection Logs

`GET /manager/fire-inspections/logs`

Filters: `extinguisherId`, `parkingId`, `floorId`, `result`, `from`, `to`, `page`, `size`.

Filter behavior:

- Omitted and empty filters are treated as no filter.
- The backend builds inspection log predicates only for supplied filters, so nullable UUID, enum, and date parameters are not bound into PostgreSQL `IS NULL OR ...` checks.
- Production fix: `from`/`to` no longer trigger PostgreSQL `could not determine data type of parameter` when no date range is supplied.

Photo fields:

- `photoUrl`: legacy URL, if submitted.
- `photoObjectKey`: private object key for uploaded inspection photos.
- `photoDisplayUrl`: presigned download URL for object-key photos, or legacy HTTP(S) `photoUrl`.
- `photoUrlExpiresInSeconds`: set for presigned display URLs.

## Staff APIs

All staff APIs require `STAFF`; tenant and parking are resolved from the approved staff kiosk/device context.

### Due Inspections

`GET /staff/fire-inspections/due`

Filters: `floorId`, `status`.

Returns extinguishers in the current staff parking with `nextInspectionAt <= now` or expiry within 30 days.

### Submit Inspection

`POST /staff/fire-inspections`

```json
{
  "fireExtinguisherId": "extinguisher-id",
  "result": "OK",
  "pressureOk": true,
  "sealOk": true,
  "locationOk": true,
  "expiryOk": true,
  "photoUrl": "https://example.com/photo.jpg",
  "photoObjectKey": "tenants/{tenantId}/fire-inspections/{staffId}/{uuid}-photo.jpg",
  "note": "Looks good",
  "nextInspectionAt": "2026-07-01T00:00:00"
}
```

Photo handling:

- Preferred flow uses `POST /staff/fire-inspections/photos/presign-upload`, browser/mobile `PUT` to the returned `uploadUrl`, then inspection submit with `photoObjectKey`.
- `photoUrl` remains accepted for backward compatibility.
- The backend does not accept binary multipart upload on inspection submit.

### Presign Inspection Photo Upload

`POST /staff/fire-inspections/photos/presign-upload`

```json
{
  "fileName": "fe-b1-001-check.jpg",
  "contentType": "image/jpeg"
}
```

Returns a presigned PUT URL and generated object key under `tenants/{tenantId}/fire-inspections/{staffId}/`. Allowed content types are `image/jpeg`, `image/png`, and `image/webp`; extension must match content type.

Status update rules:

- `OK` -> `ACTIVE`, unless `expiryDate` is already past, then `EXPIRED`
- `EXPIRED` -> `EXPIRED`
- `MISSING` -> `MISSING`
- `DAMAGED` -> `DAMAGED`
- `NEEDS_REPLACEMENT` -> `MAINTENANCE`

## Staff Available RFID Cards

`GET /staff/rfid-cards/available?search=BCONS&limit=50`

Returns active cards in the current tenant that are not attached to an `ACTIVE` parking session in the current staff kiosk parking.

```json
[
  {
    "id": "card-id",
    "code": "BCONS-0001",
    "label": "BCONS-0001",
    "status": "ACTIVE"
  }
]
```

FE should call this when opening Staff Entry and refetch after successful check-in.

## Seed Behavior

`V20260602100000__add_fire_safety_tables.sql` creates deterministic demo extinguishers for existing seeded floors:

- Codes are `FE-{parkingCodeNoSymbols}-{floorCode}-{001..006}`.
- Types rotate through `CO2`, `POWDER`, and `FOAM`.
- Coordinates are spread across the map between `5` and `95`.
- Status/date mix includes active, expiring soon, expired, due inspection, and maintenance examples.
- Seed uses `ON CONFLICT DO NOTHING` and does not overwrite manager-created data.
- A few demo inspection logs are inserted idempotently.

## Edge Cases And Limits

- Codes are unique per tenant among non-deleted extinguishers.
- Parking, floor, and zone are tenant validated. Zone must belong to the selected floor.
- `photoObjectKey` must stay under `tenants/{tenantId}/fire-inspections/`; clients cannot choose arbitrary storage folders.
- `photoUrl` is legacy-compatible; new flows should use presign upload and `photoObjectKey`.
- RFID cards are not parking-owned in the current schema, so availability is tenant-scoped and excludes cards used by active sessions in the current staff parking.

## Smoke Test Checklist

Run backend locally on port 8081 and authenticate with a manager token. Do not print or commit real tokens.

- `GET /manager/fire-extinguishers`
- `GET /manager/fire-extinguishers?search=`
- `GET /manager/fire-extinguishers?search=FE`
- `GET /manager/fire-extinguishers/summary`
- `GET /manager/floors/{floorId}/fire-safety-map`
- `GET /manager/fire-inspections/logs`
- `GET /manager/fire-inspections/logs?result=OK`
- `GET /manager/fire-inspections/logs?from=2026-01-01T00:00:00&to=2026-12-31T23:59:59`

Expected manager result: no 500 responses, no SQL type errors, success wrapper code `1000`, seeded extinguishers present, map items include `xCoordinate`/`yCoordinate`, and inspection logs work even when no rows match.

If a staff token with approved staff device/workContext is available:

- `POST /staff/fire-inspections/photos/presign-upload`
- `GET /staff/fire-inspections/due`
- `POST /staff/fire-inspections` with a safe test extinguisher
- `GET /staff/rfid-cards/available`

If no staff token/context is available, mobile staff smoke testing requires an approved staff device/workContext before these APIs can be exercised.
