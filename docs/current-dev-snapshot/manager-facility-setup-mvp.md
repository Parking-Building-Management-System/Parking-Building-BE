# Manager Facility Setup MVP

## Final Endpoint List

Parking:

- `GET /manager/parkings`
- `POST /manager/parkings`
- `GET /manager/parkings/{id}`
- `PUT /manager/parkings/{id}`
- `PATCH /manager/parkings/{id}/status`
- `GET /manager/parkings/{id}/topology`

Floor:

- `GET /manager/parkings/{parkingId}/floors`
- `POST /manager/parkings/{parkingId}/floors`
- `GET /manager/floors/{id}`
- `PUT /manager/floors/{id}`
- `DELETE /manager/floors/{id}`

Zone:

- `GET /manager/floors/{floorId}/zones`
- `POST /manager/floors/{floorId}/zones`
- `GET /manager/zones/{id}`
- `PUT /manager/zones/{id}`
- `DELETE /manager/zones/{id}`

Slot:

- `GET /manager/slots`
- `POST /manager/zones/{zoneId}/slots`
- `GET /manager/slots/{id}`
- `PUT /manager/slots/{id}`
- `DELETE /manager/slots/{id}`
- `PATCH /manager/slots/{id}/status`
- `POST /manager/slots/import`
- `GET /manager/slots/export`
- `PATCH /manager/slots/bulk-status`

RFID Cards:

- `GET /manager/rfid-cards`
- `POST /manager/rfid-cards/generate`
- `PATCH /manager/rfid-cards/{id}/status`

## Request Samples

Create parking:

```json
{
  "code": "HQ",
  "name": "Headquarters Parking",
  "address": "1 Nguyen Hue, District 1",
  "status": "ACTIVE"
}
```

Create floor:

```json
{
  "code": "B1",
  "name": "Basement 1",
  "displayOrder": 1,
  "active": true
}
```

Create zone:

```json
{
  "code": "B1-A",
  "name": "B1 Zone A",
  "vehicleTypeCode": "CAR",
  "capacity": 40,
  "status": "ACTIVE"
}
```

Create slot:

```json
{
  "code": "B1-A-001",
  "slotNumber": "001",
  "status": "AVAILABLE"
}
```

Update slot status:

```json
{
  "status": "MAINTENANCE"
}
```

Update parking status:

```json
{
  "status": "MAINTENANCE"
}
```

`PATCH /manager/parkings/{id}/status` also accepts no body for the existing toggle behavior.

Generate RFID cards:

```json
{
  "count": 100,
  "prefix": "HQ"
}
```

If `count` is missing, backend generates `max(totalSlots * 1.2, 50)`.

## Setup Flow

1. `POST /admin/tenants` creates tenant and first manager.
2. Manager first `POST /auth/login` bootstraps approved manager device.
3. Manager creates parking with `POST /manager/parkings`.
4. Manager creates floors under the parking.
5. Manager creates zones under floors.
6. Manager creates slots individually or imports them from Excel.
7. Manager generates RFID card pool for the tenant.
8. Frontend reads `GET /manager/parkings/{id}/topology` for tree/topology UI.

## DB Tables Touched

- `tenants`
- `users`
- `user_roles`
- `devices`
- `sessions`
- `parkings`
- `floors`
- `zones`
- `slots`
- `rfid_cards`
- `vehicle_types` for zone validation

## Manual SQL Validation

Facility tree:

```sql
SELECT p.code AS parking_code, f.code AS floor_code, z.code AS zone_code, s.code AS slot_code
FROM parkings p
LEFT JOIN floors f ON f.parking_id = p.id AND f.is_deleted = false
LEFT JOIN zones z ON z.floor_id = f.id AND z.is_deleted = false
LEFT JOIN slots s ON s.zone_id = z.id AND s.is_deleted = false
WHERE p.tenant_id = '<tenant-id>'
  AND p.is_deleted = false
ORDER BY p.code, f.display_order, z.code, s.code;
```

RFID pool:

```sql
SELECT status, COUNT(*)
FROM rfid_cards
WHERE tenant_id = '<tenant-id>'
GROUP BY status
ORDER BY status;
```

Duplicate guard:

```sql
SELECT tenant_id, code, COUNT(*)
FROM rfid_cards
WHERE tenant_id = '<tenant-id>'
GROUP BY tenant_id, code
HAVING COUNT(*) > 1;
```

Tenant isolation spot check:

```sql
SELECT tenant_id, COUNT(*) AS parkings
FROM parkings
WHERE is_deleted = false
GROUP BY tenant_id;
```

## Known Limitations

- Parking hard delete is not exposed.
- RFID MVP supports list, generate, and status updates only.
- Slot delete is soft delete and does not check active parking sessions yet.
- RFID cards are not assigned to users or sessions by this manager setup flow.
- Pricing/payment/PWA/kiosk/staff check-in are intentionally outside this slice.

## What Frontend Can Build Now

- Empty-tenant facility setup wizard.
- Parking/floor/zone/slot CRUD screens.
- Slot import/export workflow.
- Slot status bulk update tools.
- RFID card pool generation and status management.
- Facility topology tree for manager dashboards.
