-- Vincom single-customer demo account setup.
-- Keep tenant-scoped data model intact; only seed/fix deterministic demo auth context.

UPDATE devices
SET
    kiosk_id = md5('kiosk-vincom-dk-main')::uuid,
    updated_at = CURRENT_TIMESTAMP
WHERE fingerprint = 'seed-vincom-staff-device'
  AND kiosk_id IS NULL;

UPDATE kiosk
SET
    type = 'MIXED',
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = md5('kiosk-vincom-dk-main')::uuid;

INSERT INTO roles (id, name, "desc")
VALUES (
    md5('DEV')::uuid,
    'DEV',
    'DEV super demo role for the Vincom single-customer demo account'
)
ON CONFLICT (name) DO UPDATE
SET
    "desc" = EXCLUDED."desc",
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
SELECT
    md5('user-vincom-dev')::uuid,
    md5('tenant-vincom')::uuid,
    'dev@vincom.smartpark.local',
    staff.password,
    'Vincom DEV Demo',
    '0911000099',
    'ACTIVE',
    md5('user-vincom-manager')::uuid,
    false
FROM users staff
WHERE staff.username = 'staff@vincom.smartpark.local'
ON CONFLICT (username) DO UPDATE
SET
    tenant_id = EXCLUDED.tenant_id,
    password = EXCLUDED.password,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    created_by = EXCLUDED.created_by,
    is_deleted = EXCLUDED.is_deleted,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (id, user_id, role_id)
SELECT
    md5('dev@vincom.smartpark.local:DEV')::uuid,
    u.id,
    r.id
FROM users u
JOIN roles r ON r.name = 'DEV'
WHERE u.username = 'dev@vincom.smartpark.local'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO role_permissions (id, role_id, permission_id)
SELECT
    md5('DEV:' || p.name)::uuid,
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'DEV'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO devices (
    id, user_id, fingerprint, label, status, approved_by, approved_at, expires_at, kiosk_id
)
SELECT
    md5('dev@vincom.smartpark.local:seed-dev-vincom-device')::uuid,
    u.id,
    'seed-dev-vincom-device',
    'Vincom Dev Laptop',
    'APPROVED',
    md5('user-vincom-manager')::uuid,
    CURRENT_TIMESTAMP,
    NULL,
    md5('kiosk-vincom-dk-main')::uuid
FROM users u
WHERE u.username = 'dev@vincom.smartpark.local'
ON CONFLICT (user_id, fingerprint) DO UPDATE
SET
    label = EXCLUDED.label,
    status = EXCLUDED.status,
    approved_by = EXCLUDED.approved_by,
    approved_at = COALESCE(devices.approved_at, EXCLUDED.approved_at),
    expires_at = EXCLUDED.expires_at,
    kiosk_id = EXCLUDED.kiosk_id,
    updated_at = CURRENT_TIMESTAMP;

UPDATE kiosk_staff
SET
    is_active = true,
    updated_at = CURRENT_TIMESTAMP
WHERE tenant_id = md5('tenant-vincom')::uuid
  AND kiosk_id = md5('kiosk-vincom-dk-main')::uuid
  AND staff_user_id = (
      SELECT id
      FROM users
      WHERE username = 'dev@vincom.smartpark.local'
  )
  AND shift_id IS NULL;

INSERT INTO kiosk_staff (
    id, tenant_id, kiosk_id, staff_user_id, shift_id, assigned_at, is_active
)
SELECT
    md5('kiosk-staff-dev-vincom-dk-main')::uuid,
    md5('tenant-vincom')::uuid,
    md5('kiosk-vincom-dk-main')::uuid,
    u.id,
    NULL,
    CURRENT_TIMESTAMP,
    true
FROM users u
WHERE u.username = 'dev@vincom.smartpark.local'
  AND NOT EXISTS (
      SELECT 1
      FROM kiosk_staff ks
      WHERE ks.tenant_id = md5('tenant-vincom')::uuid
        AND ks.kiosk_id = md5('kiosk-vincom-dk-main')::uuid
        AND ks.staff_user_id = u.id
        AND ks.shift_id IS NULL
  );
