# Manager Kiosk / Device MVP

## Endpoint List

### Manager Kiosks

`GET /manager/kiosks`

Optional filters: `parkingId`, `status`, `type`.

Each kiosk response includes `assignedStaffCount`, counted from active `kiosk_staff`
assignments in the current manager tenant. Inactive assignments are excluded.

`POST /manager/kiosks`

```json
{
  "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
  "name": "Bot vao B1",
  "type": "ENTRY",
  "status": "ACTIVE"
}
```

`GET /manager/kiosks/{id}`

`PUT /manager/kiosks/{id}`

```json
{
  "name": "Bot vao B1",
  "type": "ENTRY",
  "status": "ACTIVE"
}
```

`PATCH /manager/kiosks/{id}/status`

```json
{
  "status": "INACTIVE"
}
```

`DELETE /manager/kiosks/{id}`

Soft-deletes the kiosk by setting `is_deleted = true` and `status = INACTIVE`.

### Manager Kiosk Staff

`GET /manager/kiosks/{id}/staff`

`POST /manager/kiosks/{id}/staff/{staffId}`

Idempotently creates or reactivates a no-shift kiosk staff assignment.

`DELETE /manager/kiosks/{id}/staff/{staffId}`

Deactivates active assignments for that kiosk and staff user.

### Manager Device Approvals

`GET /manager/device-approvals`

Lists pending staff devices in the manager tenant.

`POST /manager/device-approvals/{id}/approve`

```json
{
  "kioskId": "1f066b35-8db1-7b7a-9b78-967d8fcbdf3d",
  "expiresAt": null
}
```

`expiresAt: null` means permanent approval.

Temporary approval requires a future ISO timestamp:

```json
{
  "kioskId": "1f066b35-8db1-7b7a-9b78-967d8fcbdf3d",
  "expiresAt": "2026-05-27T10:00:00"
}
```

If `expiresAt` is in the past or equal to server current time, the API rejects with
`DEVICE_APPROVAL_EXPIRES_AT_MUST_BE_FUTURE`.

`POST /manager/device-approvals/{id}/reject`

`POST /manager/devices/{id}/revoke`

## Response Samples

Kiosk:

```json
{
  "id": "1f066b35-8db1-7b7a-9b78-967d8fcbdf3d",
  "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
  "parkingName": "Vincom Dong Khoi",
  "code": "VIN-DK-BOT-VAO-B1",
  "name": "Bot vao B1",
  "type": "ENTRY",
  "status": "ACTIVE",
  "assignedStaffCount": 1,
  "lastHeartbeatAt": null,
  "createdAt": "2026-05-26T17:00:00",
  "updatedAt": "2026-05-26T17:00:00"
}
```

Staff work context in `GET /auth/me`:

```json
{
  "id": "staff-user-id",
  "tenantId": "tenant-id",
  "username": "staff@tenant.local",
  "roles": ["STAFF"],
  "permissions": [],
  "workContext": {
    "kioskId": "1f066b35-8db1-7b7a-9b78-967d8fcbdf3d",
    "kioskName": "Bot vao B1",
    "kioskType": "ENTRY",
    "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
    "parkingName": "Vincom Dong Khoi"
  }
}
```

## Database Tables Touched

Existing tables reused:

- `kiosk`
- `kiosk_staff`
- `devices`
- `sessions`
- `users`
- `roles`
- `parkings`
- `parking_sessions`

Migration `V20260526180000__manager_kiosk_device_mvp.sql` adds:

- `kiosk.type`
- `kiosk.is_deleted`
- nullable `kiosk_staff.shift_id` for no-shift MVP assignments
- partial unique index for no-shift kiosk staff assignment
- `devices.kiosk_id` with FK to `kiosk(id)`

## Manager Setup Flow

1. Manager creates or selects a tenant parking.
2. Manager creates a kiosk under that parking.
3. Manager creates/selects a STAFF account.
4. Manager assigns the STAFF account to the kiosk.
5. Staff attempts login from a device fingerprint, creating a pending device when untrusted.
6. Manager approves the pending device and binds it to the assigned kiosk.

## Staff Login Flow

1. Staff submits username, password, device fingerprint, and optional label.
2. Unknown staff device is saved as `PENDING` and login fails with `DEVICE_NOT_TRUST`.
3. Approved staff device must have `devices.kiosk_id`.
4. Kiosk must belong to staff tenant and have `status = ACTIVE`.
5. Staff must have an active `kiosk_staff` assignment for the kiosk.
6. Valid login creates a session and issues tokens.

## Device Approval Flow

1. Manager calls `GET /manager/device-approvals`.
2. Manager calls `POST /manager/device-approvals/{id}/approve` with `kioskId`.
3. API verifies device tenant, kiosk tenant, and active staff-to-kiosk assignment.
4. If staff is not assigned, API returns `STAFF_NOT_ASSIGNED_TO_KIOSK`.
5. `expiresAt = null` stores a permanent approval.
6. Non-null `expiresAt` must be strictly later than server current time.
7. Approved device stores `status = APPROVED`, `approved_by`, `approved_at`, `expires_at`, and `kiosk_id`.

## Check-In ParkingId Resolution Flow

1. `POST /staff/parking-sessions/check-in` accepts the existing `parkingId` temporarily.
2. If request has `parkingId`, service uses it as the current DEV fallback.
3. If request omits `parkingId`, service resolves current JWT session.
4. Session resolves approved device, kiosk, and parking.
5. Kiosk must be ACTIVE and staff must still be assigned.
6. If no valid kiosk context exists, API returns `KIOSK_CONTEXT_REQUIRED`.
7. RFID `cardCode` behavior is unchanged.

## Known Limitations

- Kiosk staff MVP assignments use no shift. Existing shift-bound assignments remain supported by the table, but manager assignment APIs do not expose shift selection yet.
- `parkingId` remains accepted on staff check-in as a DEV fallback during frontend migration.
- Device approval lists pending devices only; approved/rejected history filtering is not implemented.
- Device revoke sets `status = SUSPENDED` and clears `kiosk_id`; it does not currently revoke already-active sessions for that device.
- Temporary approval timestamps follow the existing backend `LocalDateTime` JSON convention. Send a future ISO timestamp without relying on client-side timezone offset adjustment.
- There is no manager-level Kill Switch API yet for active staff session list, one-session revoke,
  or force logout. System Admin has security APIs, but they are not manager-scoped.
- There is no compact pending approval count endpoint; FE can count `GET /manager/device-approvals`
  until a summary/count endpoint is added.

## SQL Validation Queries

```sql
-- Kiosks and tenant parking ownership
SELECT k.id, k.tenant_id, k.parking_id, p.name AS parking_name, k.name, k.type, k.status
FROM kiosk k
JOIN parkings p ON p.id = k.parking_id
WHERE k.is_deleted = false
ORDER BY k.created_at DESC;

-- Staff assignments per kiosk
SELECT ks.kiosk_id, k.name AS kiosk_name, ks.staff_user_id, u.username, ks.is_active, ks.shift_id
FROM kiosk_staff ks
JOIN kiosk k ON k.id = ks.kiosk_id
JOIN users u ON u.id = ks.staff_user_id
ORDER BY ks.assigned_at DESC;

-- Pending staff devices
SELECT d.id, d.user_id, u.username, d.fingerprint, d.label, d.status, d.created_at
FROM devices d
JOIN users u ON u.id = d.user_id
WHERE d.status = 'PENDING'
ORDER BY d.created_at DESC;

-- Approved device work context
SELECT d.id AS device_id, u.username, d.status, d.kiosk_id, k.name AS kiosk_name,
       k.type, k.status AS kiosk_status, p.id AS parking_id, p.name AS parking_name
FROM devices d
JOIN users u ON u.id = d.user_id
LEFT JOIN kiosk k ON k.id = d.kiosk_id
LEFT JOIN parkings p ON p.id = k.parking_id
WHERE d.status = 'APPROVED';

-- Check staff assignment before approving device
SELECT COUNT(*) > 0 AS assigned
FROM kiosk_staff
WHERE tenant_id = :tenantId
  AND kiosk_id = :kioskId
  AND staff_user_id = :staffId
  AND is_active = true;
```
