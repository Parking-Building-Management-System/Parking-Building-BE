# Tenant Onboarding Device Policy

## Tenant Creation

`POST /admin/tenants` creates:

- one `tenants` row with `status = ACTIVE`
- one first manager `users` row under that tenant
- one `user_roles` row assigning `PARKING_MANAGER`

It does not create any `devices` row for the manager. The manager device is bootstrapped by
`POST /auth/login`.

## Login Decision Tree

`POST /auth/login` keeps the existing request contract:

```json
{
  "username": "manager@example.com",
  "password": "Password@123",
  "deviceFingerprint": "browser-or-device-fingerprint",
  "deviceLabel": "Manager laptop"
}
```

Decision order:

1. Verify username.
2. Verify password.
3. Reject inactive users.
4. Load roles.
5. If user is not `SYSTEM_ADMIN`, reject when tenant status is not `ACTIVE`.
6. Apply role device policy.

Role policy:

- `SYSTEM_ADMIN`: device trust does not block login. A submitted fingerprint may be stored as an
  approved device because `sessions.device_id` is currently required.
- `PARKING_MANAGER`: approved matching fingerprint succeeds. If the manager has zero approved
  devices, the submitted fingerprint is created or promoted to `APPROVED` and login succeeds. If
  the manager already has an approved device, a new fingerprint is saved as `PENDING` and login is
  rejected with the existing `DEVICE_NOT_TRUST` behavior.
- `STAFF`: strict binding. Unknown fingerprints become `PENDING`; login is rejected. Staff devices
  are never auto-approved by login.

## Expected DB Rows

Before manager first login:

- `tenants`: 1 new active tenant
- `users`: 1 active manager for that tenant
- `user_roles`: manager has `PARKING_MANAGER`
- `devices`: 0 rows for that manager
- `sessions`: 0 rows for that manager

After manager first login:

- `devices`: 1 row for manager and submitted fingerprint with `status = APPROVED`
- `devices.approved_by`: manager user id
- `devices.approved_at`: non-null
- `sessions`: 1 active row referencing that approved device

After manager login from a new fingerprint:

- login rejected with `DEVICE_NOT_TRUST`
- new device row is `PENDING` if it did not already exist
- no successful session is created for that rejected login

## Manual SQL Validation

Find tenant and first manager:

```sql
SELECT t.id AS tenant_id, t.slug, t.status, u.id AS manager_id, u.username, u.status
FROM tenants t
JOIN users u ON u.tenant_id = t.id
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE t.slug = '<tenant-slug>'
  AND r.name = 'PARKING_MANAGER';
```

Confirm no manager device exists after provisioning:

```sql
SELECT d.*
FROM devices d
WHERE d.user_id = '<manager-id>';
```

Confirm first-login bootstrap:

```sql
SELECT d.user_id, d.fingerprint, d.label, d.status, d.approved_by, d.approved_at
FROM devices d
WHERE d.user_id = '<manager-id>'
ORDER BY d.created_at;
```

Confirm session references the approved device:

```sql
SELECT s.id, s.user_id, s.device_id, d.status, s.revoked_at, s.expired_at
FROM sessions s
JOIN devices d ON d.id = s.device_id
WHERE s.user_id = '<manager-id>'
ORDER BY s.created_at DESC;
```

Confirm suspended tenant blocks non-admin login:

```sql
SELECT t.id, t.slug, t.status, u.username
FROM tenants t
JOIN users u ON u.tenant_id = t.id
WHERE t.slug = '<tenant-slug>';
```

## Known Limitations

- `sessions.device_id` is non-null, so even `SYSTEM_ADMIN` logins still need a device row for the
  submitted fingerprint.
- There is no separate manager device approval workflow yet.
- Existing pending manager devices can be promoted on first login only when the manager still has
  zero approved devices.
- Staff kiosk binding is intentionally unchanged.
