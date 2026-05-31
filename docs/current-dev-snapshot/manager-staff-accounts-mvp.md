# Manager Staff Accounts MVP

## Scope

This snapshot documents the manager-owned staff account setup slice before kiosk/device approval.

Out of scope:

- kiosk assignment
- device approval
- pricing/payment/PWA
- parking session changes
- system admin tenant provisioning changes

## Existing Staff-Related APIs

| Domain | Method | Path | Status | Notes |
|---|---|---|---|---|
| Staff operation | POST | `/staff/parking-sessions/check-in` | Existing | Staff check-in flow. Not changed by this MVP. |
| Manager staff accounts | GET | `/manager/staff` | Added | List tenant staff users. |
| Manager staff accounts | POST | `/manager/staff` | Added | Create tenant staff user and assign `STAFF`. |
| Manager staff accounts | GET | `/manager/staff/{id}` | Added | Tenant-scoped staff detail. |
| Manager staff accounts | PUT | `/manager/staff/{id}` | Added | Update basic profile fields. |
| Manager staff accounts | PATCH | `/manager/staff/{id}/status` | Added | Activate/deactivate/suspend staff. |
| Manager staff accounts | POST | `/manager/staff/{id}/reset-password` | Added | Reset password and revoke active sessions. |

## Tables Touched

- `users`: staff account identity, tenant id, status, profile fields, password hash.
- `roles`: reads `STAFF` role.
- `user_roles`: assigns staff role.
- `sessions`: active staff sessions are revoked when staff becomes inactive/suspended or password is reset.

No `devices` row is created by staff account creation. Staff device trust remains strict during login.

## Tenant Isolation Rules

- `tenantId` always comes from authenticated `PARKING_MANAGER` JWT claim `tenant_id`.
- Frontend never sends `tenantId`.
- Controller wraps all service calls with `ManagerTenantContext`.
- Staff list/detail/update/status/reset queries require both current `tenantId` and role `STAFF`.
- Manager cannot read or update staff from another tenant.
- Created staff receives only the `STAFF` role for this MVP.
- `users.username` is globally unique in the current schema, so duplicate usernames across tenants are rejected.

## Endpoint Contract

All endpoints require:

```http
Authorization: Bearer <manager-access-token>
```

### List Staff

```http
GET /manager/staff?search=nguyen&status=ACTIVE&page=0&size=20
```

Filters:

- `search` is optional.
- Blank search, for example `?search=`, is treated the same as no search.
- Search is case-insensitive.
- Search matches `username`, `fullName`, and `phone`.
- `status` is optional and accepts `ACTIVE`, `INACTIVE`, or `SUSPENDED`.
- Pagination is preserved with `page` and `size`.
- Sorting is `createdAt DESC`.

Tenant scoping:

- The manager tenant always comes from the authenticated JWT through `ManagerTenantContext`.
- The frontend must not send tenant id.
- The list always filters to current tenant users with role `STAFF` and `isDeleted = false`.

Implementation note:

- Staff list uses a Criteria/Specification query.
- Search predicates are added only when `search` is non-blank, avoiding PostgreSQL type inference
  failures such as `lower(bytea)` when an optional search parameter is null.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "items": [
      {
        "id": "11111111-1111-1111-1111-111111111111",
        "username": "acme.staff01",
        "fullName": "Nguyen Van A",
        "phone": "0900000001",
        "status": "ACTIVE",
        "createdAt": "2026-05-26T10:00:00",
        "updatedAt": "2026-05-26T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "path": "/manager/staff"
}
```

### Create Staff

```http
POST /manager/staff
```

Request:

```json
{
  "username": "acme.staff01",
  "initialPassword": "Password@123",
  "fullName": "Nguyen Van A",
  "phone": "0900000001",
  "status": "ACTIVE"
}
```

`password` is also accepted as an alias for `initialPassword`.

Behavior:

- creates `users` row under current tenant
- assigns `STAFF` role
- does not create `devices`
- default status is `ACTIVE`

### Get Staff Detail

```http
GET /manager/staff/{id}
```

Returns the same `ManagerStaffResponse` shape as list items.

### Update Staff

```http
PUT /manager/staff/{id}
```

Request:

```json
{
  "fullName": "Nguyen Van B",
  "phone": "0900000002",
  "status": "ACTIVE"
}
```

Password is not updated here.

### Update Status

```http
PATCH /manager/staff/{id}/status
```

Request:

```json
{
  "status": "INACTIVE"
}
```

If status becomes `INACTIVE` or `SUSPENDED`, active sessions for that staff user are revoked.

### Reset Password

```http
POST /manager/staff/{id}/reset-password
```

Request:

```json
{
  "newPassword": "NewPassword@123"
}
```

The password is encoded with the existing `PasswordEncoder`, and active staff sessions are revoked.

## Manual SQL Validation

Find created staff:

```sql
SELECT u.id, u.tenant_id, u.username, u.full_name, u.phone, u.status, r.name AS role
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.tenant_id = '<tenant-id>'
  AND r.name = 'STAFF'
ORDER BY u.created_at DESC;
```

Confirm no device was created:

```sql
SELECT d.*
FROM devices d
JOIN users u ON u.id = d.user_id
WHERE u.username = 'acme.staff01';
```

Confirm sessions are revoked after deactivate/reset password:

```sql
SELECT s.id, s.user_id, s.revoked_at, s.expired_at
FROM sessions s
JOIN users u ON u.id = s.user_id
WHERE u.username = 'acme.staff01'
ORDER BY s.created_at DESC;
```

Confirm username uniqueness:

```sql
SELECT username, COUNT(*)
FROM users
GROUP BY username
HAVING COUNT(*) > 1;
```

## Known Limitations

- Username is globally unique because the current `users.username` constraint is global.
- Staff account APIs do not create devices; newly created staff cannot login until a pending device
  is approved and bound to a kiosk.
- Kiosk assignment exists in the Manager Kiosk / Device MVP, but shift selection is not exposed yet.
- There is no manager-level active staff session list or force logout endpoint yet; deactivate and
  password reset still revoke active sessions for the affected staff user.
- There is no delete endpoint; use `INACTIVE` or `SUSPENDED` status.
- Password strength policy is limited to request validation and existing password encoder behavior.
