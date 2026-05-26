# Tenant Onboarding Device Policy

## Scope

This snapshot documents the current behavior for:

- `POST /admin/tenants`
- `POST /auth/login`

## Tenant Provisioning

Endpoint:

```http
POST /admin/tenants
```

Behavior:

- Creates a tenant.
- Creates the first `PARKING_MANAGER` account.
- Assigns the `PARKING_MANAGER` role.
- Does not create or bind a manager device during provisioning.

The manager device is bound during the manager's first successful login.

Implementation:

```text
src/main/java/com/smartpark/swp391/modules/admin/service/impl/AdminTenantManagementServiceImpl.java
```

## Login Device Policy

Endpoint:

```http
POST /auth/login
```

Implementation:

```text
src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/AuthenticationServiceImpl.java
src/main/java/com/smartpark/swp391/modules/identity/repository/DeviceRepository.java
```

Login always verifies username and password first.

### SYSTEM_ADMIN

- Device trust does not block login.
- If no device row exists for the submitted fingerprint, backend creates an approved device row so the existing `sessions.device_id` NOT NULL schema can still create a session.
- Tenant suspension does not block `SYSTEM_ADMIN` login.

### PARKING_MANAGER

- If the submitted fingerprint matches an approved device, login succeeds.
- If the manager has zero approved devices, backend approves the submitted fingerprint and login succeeds.
- If the manager already has at least one approved device and submits a new fingerprint, backend creates/keeps a pending device and returns `DEVICE_NOT_TRUST`.
- Suspended devices remain forbidden.

### STAFF

- Strict device binding remains.
- Unknown fingerprints are saved as `PENDING` and login is blocked with `DEVICE_NOT_TRUST`.
- Staff devices are never auto-approved.
- Only `APPROVED` staff devices can login.

## Tenant Status Rule

Non-admin users cannot login when their tenant is not `ACTIVE`.

`SYSTEM_ADMIN` is exempt from tenant status blocking so global administration remains possible.

## Verification

Run:

```bash
./mvnw test
./mvnw spring-boot:run
```

Manual checks:

- Provision a new tenant via `/admin/tenants`.
- Confirm no device is inserted for the new manager at provisioning time.
- Login as that manager with a new `deviceFingerprint`; the first login succeeds and creates an `APPROVED` device.
- Login again as that manager from another new fingerprint; login is blocked and device is `PENDING`.
- Login as staff from a new fingerprint; login is blocked and device is `PENDING`.
