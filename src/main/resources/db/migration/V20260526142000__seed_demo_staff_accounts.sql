-- Seed extra demo STAFF accounts for seeded facility tenants.
-- Default password for local/dev seeded users: Password@123
-- Devices are intentionally not seeded here; STAFF remains strict-device-bound.

WITH target_tenants(seed_key) AS (
    VALUES
        ('tenant-vincom'),
        ('tenant-fpt'),
        ('tenant-bcons')
),
tenant_managers AS (
    SELECT
        manager_user.tenant_id,
        MIN(manager_user.id::text)::uuid AS manager_user_id
    FROM users manager_user
    JOIN user_roles manager_ur
        ON manager_ur.user_id = manager_user.id
    JOIN roles manager_role
        ON manager_role.id = manager_ur.role_id
       AND manager_role.name = 'PARKING_MANAGER'
    WHERE manager_user.is_deleted = false
    GROUP BY manager_user.tenant_id
),
eligible_tenants AS (
    SELECT
        t.id AS tenant_id,
        t.slug,
        tm.manager_user_id
    FROM target_tenants tt
    JOIN tenants t
        ON t.id = md5(tt.seed_key)::uuid
       AND t.is_deleted = false
    JOIN tenant_managers tm
        ON tm.tenant_id = t.id
    WHERE EXISTS (
        SELECT 1
        FROM parkings p
        JOIN floors f
            ON f.parking_id = p.id
           AND f.tenant_id = t.id
           AND f.is_deleted = false
        JOIN zones z
            ON z.floor_id = f.id
           AND z.tenant_id = t.id
           AND z.is_deleted = false
        JOIN slots s
            ON s.zone_id = z.id
           AND s.tenant_id = t.id
           AND s.is_deleted = false
        WHERE p.tenant_id = t.id
          AND p.is_deleted = false
    )
),
staff_seed(username_suffix, full_name, phone_suffix) AS (
    VALUES
        ('staff01', 'Nguyen Van Staff 01', '0101'),
        ('staff02', 'Tran Thi Staff 02', '0102'),
        ('cashier01', 'Le Van Cashier 01', '0103')
),
staff_users AS (
    SELECT
        et.tenant_id,
        et.manager_user_id,
        ss.username_suffix || '@' || et.slug || '.smartpark.local' AS username,
        ss.full_name,
        '09' || right(replace(et.tenant_id::text, '-', ''), 6) || ss.phone_suffix AS phone
    FROM eligible_tenants et
    CROSS JOIN staff_seed ss
)
INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
SELECT
    md5('demo-staff-account:' || su.username)::uuid,
    su.tenant_id,
    su.username,
    '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
    su.full_name,
    su.phone,
    'ACTIVE',
    su.manager_user_id,
    false
FROM staff_users su
ON CONFLICT (username) DO NOTHING;

WITH target_tenants(seed_key) AS (
    VALUES
        ('tenant-vincom'),
        ('tenant-fpt'),
        ('tenant-bcons')
),
eligible_tenants AS (
    SELECT t.id AS tenant_id, t.slug
    FROM target_tenants tt
    JOIN tenants t
        ON t.id = md5(tt.seed_key)::uuid
       AND t.is_deleted = false
    WHERE EXISTS (
        SELECT 1
        FROM parkings p
        JOIN floors f
            ON f.parking_id = p.id
           AND f.tenant_id = t.id
           AND f.is_deleted = false
        JOIN zones z
            ON z.floor_id = f.id
           AND z.tenant_id = t.id
           AND z.is_deleted = false
        JOIN slots s
            ON s.zone_id = z.id
           AND s.tenant_id = t.id
           AND s.is_deleted = false
        WHERE p.tenant_id = t.id
          AND p.is_deleted = false
    )
),
staff_seed(username_suffix) AS (
    VALUES
        ('staff01'),
        ('staff02'),
        ('cashier01')
),
staff_users AS (
    SELECT ss.username_suffix || '@' || et.slug || '.smartpark.local' AS username
    FROM eligible_tenants et
    CROSS JOIN staff_seed ss
)
INSERT INTO user_roles (id, user_id, role_id)
SELECT
    md5(u.username || ':STAFF')::uuid,
    u.id,
    r.id
FROM staff_users su
JOIN users u
    ON u.username = su.username
JOIN roles r
    ON r.name = 'STAFF'
ON CONFLICT (user_id, role_id) DO NOTHING;
