# Manager Facility Map MVP

## Purpose

Managers can attach a static floor map image and place parking slots on that image. Coordinates are percentages from `0` to `100`, so the frontend can render the same coordinates on any responsive image size.

## API Contract

### Update Floor Map

`PATCH /manager/floors/{id}/map`

```json
{
  "mapImageUrl": "tenants/{tenantId}/floor-maps/{uuid}-b1-map.png"
}
```

Response:

```json
{
  "floorId": "floor-id",
  "floorName": "B1",
  "parkingId": "parking-id",
  "parkingName": "Main Parking",
  "mapImageUrl": "tenants/{tenantId}/floor-maps/{uuid}-b1-map.png"
}
```

`mapImageUrl` accepts either an `http(s)` URL or an object key under the current tenant prefix. External URLs are accepted for paste-URL setup, but they are not fetched or scanned by the backend.

### Get Floor Map

`GET /manager/floors/{id}/map`

```json
{
  "floorId": "floor-id",
  "floorName": "B1",
  "parkingId": "parking-id",
  "parkingName": "Main Parking",
  "mapImageUrl": "tenants/{tenantId}/floor-maps/{uuid}-b1-map.png",
  "coordinateMode": "PERCENT",
  "slots": [
    {
      "slotId": "slot-id",
      "slotCode": "C-15",
      "zoneId": "zone-id",
      "zoneName": "Resident Cars",
      "status": "AVAILABLE",
      "xCoordinate": 63.4,
      "yCoordinate": 42.8,
      "hasCoordinate": true
    }
  ]
}
```

### Update One Slot Coordinate

`PATCH /manager/slots/{id}/coordinate`

```json
{
  "xCoordinate": 63.4,
  "yCoordinate": 42.8
}
```

Coordinates must be `0 <= value <= 100`.

### Bulk Update Slot Coordinates

`PATCH /manager/slots/coordinates`

```json
{
  "items": [
    {
      "slotId": "slot-id",
      "xCoordinate": 63.4,
      "yCoordinate": 42.8
    }
  ]
}
```

Response:

```json
{
  "updatedCount": 1
}
```

## Frontend Flow

1. Manager requests `POST /manager/storage/presign-upload`.
2. Frontend uploads the image directly to MinIO/S3 with `PUT uploadUrl`.
3. Frontend stores the returned `objectKey` with `PATCH /manager/floors/{id}/map`.
4. Map editor loads `GET /manager/floors/{id}/map`.
5. Manager drags slots and saves with one-shot or bulk coordinate APIs.
6. PWA can later combine `mapImageUrl` and slot coordinates for driver guidance.

## Database Fields

- `floors.map_image_url VARCHAR(1000) NULL`
- `slots.x_coordinate NUMERIC(5,2) NULL`
- `slots.y_coordinate NUMERIC(5,2) NULL`

Nullable coordinates mean the slot has not been placed on the map.

## Manual Test

Patch a floor map:

```bash
curl -X PATCH "$BASE_URL/manager/floors/$FLOOR_ID/map" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"mapImageUrl\":\"$OBJECT_KEY\"}"
```

Read map setup:

```bash
curl "$BASE_URL/manager/floors/$FLOOR_ID/map" \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

Patch one coordinate:

```bash
curl -X PATCH "$BASE_URL/manager/slots/$SLOT_ID/coordinate" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"xCoordinate":63.4,"yCoordinate":42.8}'
```

Invalid coordinate check:

```bash
curl -X PATCH "$BASE_URL/manager/slots/$SLOT_ID/coordinate" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"xCoordinate":101,"yCoordinate":42.8}'
```

Expected result: HTTP 400 validation/API error.

## Known Limitations

- No PWA route rendering yet.
- No indoor pathfinding.
- No image moderation, malware scanning, or server-side fetch validation for external URLs.
- Private MinIO objects should be displayed through presigned download URLs unless a public bucket policy is intentionally added later.
- Facility overview does not have a mapped-slot count endpoint; FE can count
  `GET /manager/floors/{id}/map.slots[*].hasCoordinate` per floor.
