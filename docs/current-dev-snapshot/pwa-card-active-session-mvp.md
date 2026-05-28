# PWA Card Active Session MVP

## QR Strategy

The QR belongs to the RFID card, not to a parking session.

`rfid_cards.qr_token` is the public opaque token encoded in the QR. It is globally unique and is not derived from the sequential card code. The card code remains operational staff-facing data only.

Possession of the physical card QR grants access to the current active session guide for that card. This is acceptable for the MVP because the driver holding the card needs to find the assigned slot. A future version can add signed or rotating tokens if the access model needs stricter controls.

## Database

Field added:

```sql
rfid_cards.qr_token VARCHAR(120) NOT NULL UNIQUE
```

Existing RFID cards are backfilled with opaque `qr_...` tokens. Newly generated cards receive random URL-safe tokens from the backend.

## Staff Check-In Response

`POST /staff/parking-sessions/check-in` keeps the existing request behavior and adds QR/PWA fields.

Sample response payload:

```json
{
  "sessionId": "session-id",
  "plateNumber": "30A-12345",
  "cardCode": "BCONS-0004",
  "qrToken": "opaque-token",
  "pwaAccessPath": "/pwa/c/opaque-token",
  "assignedSlotId": "slot-id",
  "assignedSlotCode": "C-15",
  "zoneId": "zone-id",
  "zoneName": "B1 Resident Cars",
  "parkingId": "parking-id",
  "entryTime": "2026-05-28T10:00:00",
  "status": "ACTIVE"
}
```

No frontend base URL config exists yet, so the backend returns the relative `pwaAccessPath` only.

## Public PWA Endpoint

`GET /pwa/cards/{qrToken}/active-session`

No authentication is required.

Response:

```json
{
  "sessionId": "session-id",
  "plateNumber": "30A-12345",
  "licensePlate": "30A-12345",
  "cardCode": "BCONS-0004",
  "checkInAt": "2026-05-28T10:00:00",
  "parkingId": "parking-id",
  "parkingName": "Bcons Plaza Residential Parking",
  "floorId": "floor-id",
  "floorName": "B1",
  "zoneId": "zone-id",
  "zoneName": "Resident Cars",
  "slotId": "slot-id",
  "slotCode": "C-15",
  "xCoordinate": 63.4,
  "yCoordinate": 42.8,
  "coordinateMode": "PERCENT",
  "mapImageUrl": "tenants/{tenantId}/floor-maps/{uuid}-b1-map.png",
  "mapDisplayUrl": "http://localhost:9000/smartpark/tenants/{tenantId}/floor-maps/{uuid}-b1-map.png?...",
  "mapUrlExpiresInSeconds": 900,
  "status": "ACTIVE",
  "guideText": "Xe cua ban o tang B1, khu Resident Cars, slot C-15."
}
```

Map URL behavior:

- `mapImageUrl` is the original value stored on the floor. It can be an `http(s)` URL or a MinIO/S3 object key such as `tenants/{tenantId}/floor-maps/{uuid}-b1-map.png`.
- `mapDisplayUrl` is the browser-displayable URL the public PWA should use for rendering the map image.
- If `mapImageUrl` already starts with `http://` or `https://`, `mapDisplayUrl` equals `mapImageUrl` and `mapUrlExpiresInSeconds` is `null`.
- If `mapImageUrl` is an object key, the backend creates a presigned download URL with the existing storage service and returns it as `mapDisplayUrl`.
- For MinIO/S3 object keys, `mapUrlExpiresInSeconds` is currently `900` seconds.

If the floor map or slot coordinates have not been configured yet:

- `mapImageUrl` is `null`
- `mapDisplayUrl` is `null`
- `xCoordinate` and `yCoordinate` are `null`
- `guideText` still includes floor/zone/slot information where available

The public PWA endpoint does not expose an arbitrary MinIO download API. It only creates a display URL for the floor map attached to the active parking session resolved from the card QR token. The caller never supplies `tenantId` or `objectKey` as lookup input, and MinIO access keys/secrets are never returned.

## Error Cases

- Invalid or unknown token: `404`, message `CARD_QR_NOT_FOUND`
- Card status is not `ACTIVE`: `403`, message `CARD_NOT_ACTIVE`
- Card exists but has no active parking session: `404`, message `NO_ACTIVE_SESSION_FOR_CARD`

## Manual Test

Find a card token:

```sql
SELECT code, qr_token
FROM rfid_cards
WHERE qr_token IS NOT NULL
ORDER BY code
LIMIT 5;
```

Call the public endpoint:

```bash
curl "$BASE_URL/pwa/cards/$QR_TOKEN/active-session"
```

For object-key floor maps, confirm the response contains a display URL:

```bash
MAP_DISPLAY_URL=$(curl -s "$BASE_URL/pwa/cards/$QR_TOKEN/active-session" \
  | jq -r '.result.mapDisplayUrl')

curl -I "$MAP_DISPLAY_URL"
```

Expected manual validation:

- MinIO/S3 env is configured: `APP_MINIO_ENDPOINT`, `APP_MINIO_ACCESS_KEY`, `APP_MINIO_SECRET_KEY`, `APP_MINIO_SIGNING_REGION`, and `APP_MINIO_BUCKET`.
- The floor has `mapImageUrl` stored as an object key under the tenant prefix.
- The assigned slot has `xCoordinate` and `yCoordinate`.
- Staff check-in returns `qrToken` and `pwaAccessPath`.
- `GET /pwa/cards/{qrToken}/active-session` works without an `Authorization` header.
- The response includes `mapImageUrl`, `mapDisplayUrl`, `mapUrlExpiresInSeconds`, `xCoordinate`, `yCoordinate`, and `coordinateMode`.
- Opening or curling `mapDisplayUrl` returns the image without backend authentication.
- An invalid QR token still returns `CARD_QR_NOT_FOUND`.
- A card with no active parking session still returns `NO_ACTIVE_SESSION_FOR_CARD`.

Staff check-in should expose the card QR fields:

```bash
curl -X POST "$BASE_URL/staff/parking-sessions/check-in" \
  -H "Authorization: Bearer $STAFF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "plateNumber": "30A-12345",
    "cardCode": "BCONS-0004",
    "parkingId": "parking-id"
  }'
```

## Known Limitations

- No PWA frontend in this slice.
- No checkout/payment/OTP/pathfinding.
- The endpoint exposes only active-session guide data, not user identity or billing data.
- QR token is static per card for MVP.
