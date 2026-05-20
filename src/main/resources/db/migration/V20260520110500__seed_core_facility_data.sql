INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES
    (
        md5('tenant-vincom')::uuid,
        'Vincom Mega Mall',
        'vincom-mega-mall',
        'ops@vincom.smartpark.local',
        'ACTIVE',
        false
    ),
    (
        md5('tenant-fpt')::uuid,
        'FPT Tower',
        'fpt-tower',
        'ops@fpttower.smartpark.local',
        'ACTIVE',
        false
    ),
    (
        md5('tenant-bcons')::uuid,
        'Bcons Plaza',
        'bcons-plaza',
        'ops@bcons.smartpark.local',
        'ACTIVE',
        false
    )
ON CONFLICT (id) DO UPDATE
SET
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    email_contact = EXCLUDED.email_contact,
    status = EXCLUDED.status,
    is_deleted = EXCLUDED.is_deleted;

INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    (
        md5('user-vincom-manager')::uuid,
        md5('tenant-vincom')::uuid,
        'manager@vincom.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Nguyen Minh Anh',
        '0911000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('user-vincom-staff')::uuid,
        md5('tenant-vincom')::uuid,
        'staff@vincom.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Tran Bao Long',
        '0911000002',
        'ACTIVE',
        md5('user-vincom-manager')::uuid,
        false
    ),
    (
        md5('user-fpt-manager')::uuid,
        md5('tenant-fpt')::uuid,
        'manager@fpt.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Le Quang Huy',
        '0922000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('user-fpt-staff')::uuid,
        md5('tenant-fpt')::uuid,
        'staff@fpt.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Pham Gia Khang',
        '0922000002',
        'ACTIVE',
        md5('user-fpt-manager')::uuid,
        false
    ),
    (
        md5('user-bcons-manager')::uuid,
        md5('tenant-bcons')::uuid,
        'manager@bcons.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Do Thuy Linh',
        '0933000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('user-bcons-staff')::uuid,
        md5('tenant-bcons')::uuid,
        'staff@bcons.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Hoang Viet Dung',
        '0933000002',
        'ACTIVE',
        md5('user-bcons-manager')::uuid,
        false
    )
ON CONFLICT (username) DO UPDATE
SET
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    is_deleted = EXCLUDED.is_deleted;

INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (md5('manager@vincom.smartpark.local:PARKING_MANAGER')::uuid, md5('user-vincom-manager')::uuid, md5('PARKING_MANAGER')::uuid),
    (md5('staff@vincom.smartpark.local:STAFF')::uuid, md5('user-vincom-staff')::uuid, md5('STAFF')::uuid),
    (md5('manager@fpt.smartpark.local:PARKING_MANAGER')::uuid, md5('user-fpt-manager')::uuid, md5('PARKING_MANAGER')::uuid),
    (md5('staff@fpt.smartpark.local:STAFF')::uuid, md5('user-fpt-staff')::uuid, md5('STAFF')::uuid),
    (md5('manager@bcons.smartpark.local:PARKING_MANAGER')::uuid, md5('user-bcons-manager')::uuid, md5('PARKING_MANAGER')::uuid),
    (md5('staff@bcons.smartpark.local:STAFF')::uuid, md5('user-bcons-staff')::uuid, md5('STAFF')::uuid)
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO devices (id, user_id, fingerprint, label, status, approved_by, approved_at, expires_at)
VALUES
    (md5('manager@vincom.smartpark.local:seed-vincom-manager-device')::uuid, md5('user-vincom-manager')::uuid, 'seed-vincom-manager-device', 'Vincom Manager Laptop', 'APPROVED', md5('system.admin@smartpark.local')::uuid, CURRENT_TIMESTAMP, NULL),
    (md5('staff@vincom.smartpark.local:seed-vincom-staff-device')::uuid, md5('user-vincom-staff')::uuid, 'seed-vincom-staff-device', 'Vincom Staff Tablet', 'APPROVED', md5('user-vincom-manager')::uuid, CURRENT_TIMESTAMP, NULL),
    (md5('manager@fpt.smartpark.local:seed-fpt-manager-device')::uuid, md5('user-fpt-manager')::uuid, 'seed-fpt-manager-device', 'FPT Manager Laptop', 'APPROVED', md5('system.admin@smartpark.local')::uuid, CURRENT_TIMESTAMP, NULL),
    (md5('staff@fpt.smartpark.local:seed-fpt-staff-device')::uuid, md5('user-fpt-staff')::uuid, 'seed-fpt-staff-device', 'FPT Gate Tablet', 'APPROVED', md5('user-fpt-manager')::uuid, CURRENT_TIMESTAMP, NULL),
    (md5('manager@bcons.smartpark.local:seed-bcons-manager-device')::uuid, md5('user-bcons-manager')::uuid, 'seed-bcons-manager-device', 'Bcons Manager Laptop', 'APPROVED', md5('system.admin@smartpark.local')::uuid, CURRENT_TIMESTAMP, NULL),
    (md5('staff@bcons.smartpark.local:seed-bcons-staff-device')::uuid, md5('user-bcons-staff')::uuid, 'seed-bcons-staff-device', 'Bcons Guard Tablet', 'APPROVED', md5('user-bcons-manager')::uuid, CURRENT_TIMESTAMP, NULL)
ON CONFLICT (user_id, fingerprint) DO NOTHING;

INSERT INTO vehicle_types (id, name, code, is_active, is_deleted)
VALUES
    (md5('vehicle-type-car')::uuid, 'Car', 'CAR', true, false),
    (md5('vehicle-type-motorbike')::uuid, 'Motorcycle', 'MOTORBIKE', true, false),
    (md5('vehicle-type-electric-car')::uuid, 'Electric Car', 'ELECTRIC_CAR', true, false)
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    is_active = EXCLUDED.is_active,
    is_deleted = EXCLUDED.is_deleted;

INSERT INTO parkings (id, tenant_id, code, name, address, total_capacity, status, is_deleted)
VALUES
    (
        md5('parking-vincom-dongkhoi')::uuid,
        md5('tenant-vincom')::uuid,
        'VINCOM-DK',
        'Vincom Dong Khoi',
        '72 Le Thanh Ton, District 1, Ho Chi Minh City',
        0,
        'ACTIVE',
        false
    ),
    (
        md5('parking-vincom-thaodien')::uuid,
        md5('tenant-vincom')::uuid,
        'VINCOM-TD',
        'Vincom Thao Dien',
        '161 Xa Lo Ha Noi, Thao Dien, Thu Duc City',
        0,
        'MAINTENANCE',
        false
    ),
    (
        md5('parking-fpt-tower')::uuid,
        md5('tenant-fpt')::uuid,
        'FPT-TOWER',
        'FPT Tower Smart Parking',
        '10 Pham Van Bach, Cau Giay, Hanoi',
        0,
        'ACTIVE',
        false
    ),
    (
        md5('parking-bcons-plaza')::uuid,
        md5('tenant-bcons')::uuid,
        'BCONS-PLAZA',
        'Bcons Plaza Residential Parking',
        'Bcons Plaza, Di An, Binh Duong',
        0,
        'ACTIVE',
        false
    )
ON CONFLICT (tenant_id, code) DO UPDATE
SET
    name = EXCLUDED.name,
    address = EXCLUDED.address,
    status = EXCLUDED.status,
    is_deleted = EXCLUDED.is_deleted;

WITH zone_seed(tenant_key, parking_key, zone_key, code, name, floor_name, capacity, status) AS (
    VALUES
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-a', 'B1-A', 'B1 Zone A - Premium Cars', 'B1', 20, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-b', 'B1-B', 'B1 Zone B - Motorcycles', 'B1', 30, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-e', 'B1-E', 'B1 Zone E - EV Chargers', 'B1', 12, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-a', 'B2-A', 'B2 Zone A - Family Cars', 'B2', 20, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-b', 'B2-B', 'B2 Zone B - Motorcycles', 'B2', 30, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-e', 'B2-E', 'B2 Zone E - EV Chargers', 'B2', 12, 'MAINTENANCE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-a', 'B3-A', 'B3 Zone A - Long Stay Cars', 'B3', 20, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-b', 'B3-B', 'B3 Zone B - Staff Motorcycles', 'B3', 30, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-e', 'B3-E', 'B3 Zone E - EV Overflow', 'B3', 12, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-a', 'B1-A', 'B1 Zone A - Retail Cars', 'B1', 18, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-b', 'B1-B', 'B1 Zone B - Motorcycles', 'B1', 28, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-e', 'B1-E', 'B1 Zone E - EV Chargers', 'B1', 10, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-a', 'B2-A', 'B2 Zone A - Cinema Cars', 'B2', 18, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-b', 'B2-B', 'B2 Zone B - Motorcycles', 'B2', 28, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-e', 'B2-E', 'B2 Zone E - EV Chargers', 'B2', 10, 'MAINTENANCE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-a', 'B3-A', 'B3 Zone A - Long Stay Cars', 'B3', 18, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-b', 'B3-B', 'B3 Zone B - Staff Motorcycles', 'B3', 28, 'ACTIVE'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-e', 'B3-E', 'B3 Zone E - EV Overflow', 'B3', 10, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-lab', 'G-LAB', 'Ground Zone LAB - Visitor Cars', 'G', 36, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-bike', 'G-BIKE', 'Ground Zone Bike Dock', 'G', 60, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-ev', 'G-EV', 'Ground Zone EV Smart Chargers', 'G', 18, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l1-staff', 'L1-STAFF', 'Level 1 Staff Cars', 'L1', 42, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l1-bike', 'L1-BIKE', 'Level 1 Motorbike Lab', 'L1', 70, 'ACTIVE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l2-data', 'L2-DATA', 'Level 2 Data Center Reserved Cars', 'L2', 30, 'MAINTENANCE'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l2-ev', 'L2-EV', 'Level 2 Fleet EV Chargers', 'L2', 24, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-car', 'B1-CAR', 'B1 Resident Cars', 'B1', 32, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-bike', 'B1-BIKE', 'B1 Resident Motorcycles', 'B1', 54, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-ev', 'B1-EV', 'B1 Resident EV Charging', 'B1', 12, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-car', 'B2-CAR', 'B2 Visitor Cars', 'B2', 28, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-bike', 'B2-BIKE', 'B2 Visitor Motorcycles', 'B2', 48, 'ACTIVE'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-ev', 'B2-EV', 'B2 EV Overflow', 'B2', 10, 'MAINTENANCE')
)
INSERT INTO zones (id, tenant_id, parking_id, code, name, floor_name, capacity, status, is_deleted)
SELECT
    md5('zone:' || zone_key)::uuid,
    md5(tenant_key)::uuid,
    md5(parking_key)::uuid,
    code,
    name,
    floor_name,
    capacity,
    status,
    false
FROM zone_seed
ON CONFLICT (tenant_id, parking_id, code) DO UPDATE
SET
    name = EXCLUDED.name,
    floor_name = EXCLUDED.floor_name,
    capacity = EXCLUDED.capacity,
    status = EXCLUDED.status,
    is_deleted = EXCLUDED.is_deleted;

WITH zone_seed(tenant_key, parking_key, zone_key, code, floor_name, capacity, slot_prefix) AS (
    VALUES
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-a', 'B1-A', 'B1', 20, 'A-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-b', 'B1-B', 'B1', 30, 'B-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b1-e', 'B1-E', 'B1', 12, 'E-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-a', 'B2-A', 'B2', 20, 'A-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-b', 'B2-B', 'B2', 30, 'B-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b2-e', 'B2-E', 'B2', 12, 'E-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-a', 'B3-A', 'B3', 20, 'A-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-b', 'B3-B', 'B3', 30, 'B-'),
        ('tenant-vincom', 'parking-vincom-dongkhoi', 'vincom-dk:b3-e', 'B3-E', 'B3', 12, 'E-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-a', 'B1-A', 'B1', 18, 'A-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-b', 'B1-B', 'B1', 28, 'B-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b1-e', 'B1-E', 'B1', 10, 'E-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-a', 'B2-A', 'B2', 18, 'A-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-b', 'B2-B', 'B2', 28, 'B-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b2-e', 'B2-E', 'B2', 10, 'E-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-a', 'B3-A', 'B3', 18, 'A-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-b', 'B3-B', 'B3', 28, 'B-'),
        ('tenant-vincom', 'parking-vincom-thaodien', 'vincom-td:b3-e', 'B3-E', 'B3', 10, 'E-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-lab', 'G-LAB', 'G', 36, 'LAB-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-bike', 'G-BIKE', 'G', 60, 'BIKE-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:g-ev', 'G-EV', 'G', 18, 'EV-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l1-staff', 'L1-STAFF', 'L1', 42, 'STF-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l1-bike', 'L1-BIKE', 'L1', 70, 'MOTO-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l2-data', 'L2-DATA', 'L2', 30, 'DATA-'),
        ('tenant-fpt', 'parking-fpt-tower', 'fpt:l2-ev', 'L2-EV', 'L2', 24, 'EV-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-car', 'B1-CAR', 'B1', 32, 'C-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-bike', 'B1-BIKE', 'B1', 54, 'M-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b1-ev', 'B1-EV', 'B1', 12, 'EV-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-car', 'B2-CAR', 'B2', 28, 'V-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-bike', 'B2-BIKE', 'B2', 48, 'M-'),
        ('tenant-bcons', 'parking-bcons-plaza', 'bcons:b2-ev', 'B2-EV', 'B2', 10, 'EV-')
),
slot_seed AS (
    SELECT
        zone_seed.*,
        slot_no,
        slot_prefix || lpad(slot_no::text, 2, '0') AS slot_code,
        CASE
            WHEN slot_no % 29 = 0 THEN 'LOCKED'
            WHEN slot_no % 17 = 0 THEN 'MAINTENANCE'
            WHEN slot_no % 5 = 0 OR slot_no % 7 = 0 THEN 'OCCUPIED'
            ELSE 'AVAILABLE'
        END AS slot_status
    FROM zone_seed
    CROSS JOIN LATERAL generate_series(1, zone_seed.capacity) AS generated(slot_no)
)
INSERT INTO slots (id, tenant_id, parking_id, zone_id, code, slot_number, status, is_deleted)
SELECT
    md5('slot:' || zone_key || ':' || slot_no)::uuid,
    md5(tenant_key)::uuid,
    md5(parking_key)::uuid,
    md5('zone:' || zone_key)::uuid,
    slot_code,
    floor_name || '-' || slot_code,
    slot_status,
    false
FROM slot_seed
ON CONFLICT (tenant_id, zone_id, code) DO UPDATE
SET
    slot_number = EXCLUDED.slot_number,
    status = EXCLUDED.status,
    is_deleted = EXCLUDED.is_deleted;

UPDATE slots
SET is_deleted = true
WHERE zone_id IN (md5('zone-vincom-b1')::uuid, md5('zone-fpt-a')::uuid);

UPDATE zones
SET is_deleted = true
WHERE id IN (md5('zone-vincom-b1')::uuid, md5('zone-fpt-a')::uuid);

UPDATE parkings
SET is_deleted = true
WHERE id = md5('parking-fpt-campus')::uuid;

UPDATE parkings p
SET total_capacity = slot_counts.total_capacity
FROM (
    SELECT parking_id, COUNT(*)::integer AS total_capacity
    FROM slots
    WHERE is_deleted = false
    GROUP BY parking_id
) slot_counts
WHERE p.id = slot_counts.parking_id;

INSERT INTO rfid_cards (id, tenant_id, code, uid, assigned_user_id, status, activated_at, expired_at)
VALUES
    (md5('rfid-vincom-manager')::uuid, md5('tenant-vincom')::uuid, 'VIN-RFID-001', 'VIN-UID-001', md5('user-vincom-manager')::uuid, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '335 days'),
    (md5('rfid-fpt-manager')::uuid, md5('tenant-fpt')::uuid, 'FPT-RFID-001', 'FPT-UID-001', md5('user-fpt-manager')::uuid, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP + INTERVAL '320 days'),
    (md5('rfid-bcons-manager')::uuid, md5('tenant-bcons')::uuid, 'BCONS-RFID-001', 'BCONS-UID-001', md5('user-bcons-manager')::uuid, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '345 days')
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO user_vehicle_link (
    id, tenant_id, user_id, vehicle_type_id, license_plate, vehicle_label,
    is_default, is_active, is_deleted
)
VALUES
    (md5('uvl-vincom-car')::uuid, md5('tenant-vincom')::uuid, md5('user-vincom-manager')::uuid, md5('vehicle-type-car')::uuid, '51A-12345', 'Vincom Manager Sedan', true, true, false),
    (md5('uvl-fpt-ev')::uuid, md5('tenant-fpt')::uuid, md5('user-fpt-manager')::uuid, md5('vehicle-type-electric-car')::uuid, '30E-20260', 'FPT Electric Fleet Car', true, true, false),
    (md5('uvl-bcons-bike')::uuid, md5('tenant-bcons')::uuid, md5('user-bcons-manager')::uuid, md5('vehicle-type-motorbike')::uuid, '61B1-54321', 'Bcons Resident Bike', true, true, false)
ON CONFLICT (tenant_id, license_plate) DO NOTHING;

INSERT INTO shift (id, tenant_id, parking_id, name, start_time, end_time, is_active)
VALUES
    (md5('shift-vincom-dk-morning')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-dongkhoi')::uuid, 'Dong Khoi Morning', TIME '08:00', TIME '16:00', true),
    (md5('shift-vincom-dk-night')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-dongkhoi')::uuid, 'Dong Khoi Night', TIME '16:00', TIME '23:59', true),
    (md5('shift-vincom-td-morning')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-thaodien')::uuid, 'Thao Dien Morning', TIME '08:00', TIME '16:00', true),
    (md5('shift-fpt-tower-morning')::uuid, md5('tenant-fpt')::uuid, md5('parking-fpt-tower')::uuid, 'FPT Tower Morning', TIME '07:00', TIME '15:00', true),
    (md5('shift-fpt-tower-night')::uuid, md5('tenant-fpt')::uuid, md5('parking-fpt-tower')::uuid, 'Night Ops', TIME '15:00', TIME '23:00', true),
    (md5('shift-bcons-day')::uuid, md5('tenant-bcons')::uuid, md5('parking-bcons-plaza')::uuid, 'Residential Day', TIME '06:00', TIME '18:00', true)
ON CONFLICT (tenant_id, parking_id, name) DO NOTHING;

INSERT INTO kiosk (id, tenant_id, parking_id, code, name, status, last_heartbeat_at)
VALUES
    (md5('kiosk-vincom-dk-main')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-dongkhoi')::uuid, 'VIN-DK-GATE-01', 'Dong Khoi Main Gate', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '1 minute'),
    (md5('kiosk-vincom-td-main')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-thaodien')::uuid, 'VIN-TD-GATE-01', 'Thao Dien River Gate', 'MAINTENANCE', CURRENT_TIMESTAMP - INTERVAL '18 minutes'),
    (md5('kiosk-fpt-tower-main')::uuid, md5('tenant-fpt')::uuid, md5('parking-fpt-tower')::uuid, 'FPT-AI-GATE-01', 'FPT AI Gate', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '2 minutes'),
    (md5('kiosk-bcons-main')::uuid, md5('tenant-bcons')::uuid, md5('parking-bcons-plaza')::uuid, 'BCONS-GATE-01', 'Bcons Resident Gate', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '3 minutes')
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO kiosk_staff (id, tenant_id, kiosk_id, staff_user_id, shift_id, assigned_at, is_active)
VALUES
    (md5('kiosk-staff-vincom-dk-morning')::uuid, md5('tenant-vincom')::uuid, md5('kiosk-vincom-dk-main')::uuid, md5('user-vincom-staff')::uuid, md5('shift-vincom-dk-morning')::uuid, CURRENT_TIMESTAMP - INTERVAL '1 day', true),
    (md5('kiosk-staff-fpt-tower-morning')::uuid, md5('tenant-fpt')::uuid, md5('kiosk-fpt-tower-main')::uuid, md5('user-fpt-staff')::uuid, md5('shift-fpt-tower-morning')::uuid, CURRENT_TIMESTAMP - INTERVAL '1 day', true),
    (md5('kiosk-staff-bcons-day')::uuid, md5('tenant-bcons')::uuid, md5('kiosk-bcons-main')::uuid, md5('user-bcons-staff')::uuid, md5('shift-bcons-day')::uuid, CURRENT_TIMESTAMP - INTERVAL '1 day', true)
ON CONFLICT (tenant_id, kiosk_id, staff_user_id, shift_id) DO NOTHING;

INSERT INTO parking_sessions (
    id, tenant_id, parking_id, zone_id, slot_id, rfid_card_id, vehicle_type_id,
    user_vehicle_link_id, license_plate, check_in_at, check_out_at, status,
    entry_image_url, exit_image_url, total_amount
)
VALUES
    (md5('ps-vincom-active')::uuid, md5('tenant-vincom')::uuid, md5('parking-vincom-dongkhoi')::uuid, md5('zone:vincom-dk:b1-a')::uuid, md5('slot:vincom-dk:b1-a:5')::uuid, md5('rfid-vincom-manager')::uuid, md5('vehicle-type-car')::uuid, md5('uvl-vincom-car')::uuid, '51A-12345', CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL, 'ACTIVE', NULL, NULL, NULL),
    (md5('ps-fpt-completed')::uuid, md5('tenant-fpt')::uuid, md5('parking-fpt-tower')::uuid, md5('zone:fpt:g-ev')::uuid, md5('slot:fpt:g-ev:5')::uuid, md5('rfid-fpt-manager')::uuid, md5('vehicle-type-electric-car')::uuid, md5('uvl-fpt-ev')::uuid, '30E-20260', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '1 hour', 'COMPLETED', NULL, NULL, 35000.00),
    (md5('ps-bcons-active')::uuid, md5('tenant-bcons')::uuid, md5('parking-bcons-plaza')::uuid, md5('zone:bcons:b1-bike')::uuid, md5('slot:bcons:b1-bike:5')::uuid, md5('rfid-bcons-manager')::uuid, md5('vehicle-type-motorbike')::uuid, md5('uvl-bcons-bike')::uuid, '61B1-54321', CURRENT_TIMESTAMP - INTERVAL '55 minutes', NULL, 'ACTIVE', NULL, NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO subscriptions (
    id, tenant_id, user_id, user_vehicle_link_id, parking_id, period_string,
    start_date, end_date, monthly_price, status, auto_renew
)
VALUES
    (md5('sub-vincom-active-202605')::uuid, md5('tenant-vincom')::uuid, md5('user-vincom-manager')::uuid, md5('uvl-vincom-car')::uuid, md5('parking-vincom-dongkhoi')::uuid, '2026-05', DATE '2026-05-01', DATE '2026-05-31', 2500000.00, 'ACTIVE', true),
    (md5('sub-fpt-active-202605')::uuid, md5('tenant-fpt')::uuid, md5('user-fpt-manager')::uuid, md5('uvl-fpt-ev')::uuid, md5('parking-fpt-tower')::uuid, '2026-05', DATE '2026-05-01', DATE '2026-05-31', 1800000.00, 'ACTIVE', true),
    (md5('sub-bcons-active-202605')::uuid, md5('tenant-bcons')::uuid, md5('user-bcons-manager')::uuid, md5('uvl-bcons-bike')::uuid, md5('parking-bcons-plaza')::uuid, '2026-05', DATE '2026-05-01', DATE '2026-05-31', 350000.00, 'ACTIVE', false)
ON CONFLICT DO NOTHING;

INSERT INTO invoice (
    id, tenant_id, invoice_no, user_id, subscription_id, parking_session_id,
    invoice_type, status, amount, tax_amount, total_amount, issued_at, due_at, paid_at
)
VALUES
    (md5('invoice-vincom-sub-202605')::uuid, md5('tenant-vincom')::uuid, 'VIN-INV-202605-001', md5('user-vincom-manager')::uuid, md5('sub-vincom-active-202605')::uuid, NULL, 'SUBSCRIPTION', 'PAID', 2500000.00, 0.00, 2500000.00, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (md5('invoice-fpt-session-001')::uuid, md5('tenant-fpt')::uuid, 'FPT-INV-202605-001', md5('user-fpt-manager')::uuid, NULL, md5('ps-fpt-completed')::uuid, 'PARKING_SESSION', 'ISSUED', 35000.00, 0.00, 35000.00, CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '7 days', NULL),
    (md5('invoice-bcons-sub-202605')::uuid, md5('tenant-bcons')::uuid, 'BCONS-INV-202605-001', md5('user-bcons-manager')::uuid, md5('sub-bcons-active-202605')::uuid, NULL, 'SUBSCRIPTION', 'DRAFT', 350000.00, 0.00, 350000.00, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '5 days', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO zone_violation_report (
    id, tenant_id, parking_session_id, zone_id, slot_id, reported_by, violation_type,
    description, status, occurred_at, resolved_at
)
VALUES
    (md5('zvr-vincom-wrong-zone')::uuid, md5('tenant-vincom')::uuid, md5('ps-vincom-active')::uuid, md5('zone:vincom-dk:b1-a')::uuid, md5('slot:vincom-dk:b1-a:5')::uuid, md5('user-vincom-staff')::uuid, 'WRONG_ZONE', 'Vehicle parked in a premium aisle without matching reservation.', 'OPEN', CURRENT_TIMESTAMP - INTERVAL '40 minutes', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO webhook_log (
    id, tenant_id, provider, event_type, external_id, payload, status,
    received_at, processed_at, error_message
)
VALUES
    (md5('webhook-vincom-payment-success')::uuid, md5('tenant-vincom')::uuid, 'MOMO', 'payment.success', 'momo-vincom-001', '{"invoiceNo":"VIN-INV-202605-001","amount":2500000}', 'PROCESSED', CURRENT_TIMESTAMP - INTERVAL '19 days', CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '1 minute', NULL),
    (md5('webhook-fpt-payment-pending')::uuid, md5('tenant-fpt')::uuid, 'VNPAY', 'payment.created', 'vnpay-fpt-001', '{"invoiceNo":"FPT-INV-202605-001","amount":35000}', 'RECEIVED', CURRENT_TIMESTAMP - INTERVAL '30 minutes', NULL, NULL),
    (md5('webhook-bcons-payment-draft')::uuid, md5('tenant-bcons')::uuid, 'BANK_TRANSFER', 'invoice.created', 'bank-bcons-001', '{"invoiceNo":"BCONS-INV-202605-001","amount":350000}', 'RECEIVED', CURRENT_TIMESTAMP - INTERVAL '2 days', NULL, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO audit_logs (
    id, tenant_id, actor_user_id, action, resource_type, resource_id,
    ip_address, user_agent, metadata, occurred_at
)
VALUES
    (md5('audit-vincom-rich-seed')::uuid, md5('tenant-vincom')::uuid, md5('user-vincom-manager')::uuid, 'SEED_FACILITY', 'Parking', md5('parking-vincom-dongkhoi')::uuid, '10.10.1.10', 'SmartPark Seed', '{"source":"mega-rich-seed","buildings":2}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (md5('audit-fpt-rich-seed')::uuid, md5('tenant-fpt')::uuid, md5('user-fpt-manager')::uuid, 'SEED_FACILITY', 'Parking', md5('parking-fpt-tower')::uuid, '10.20.1.10', 'SmartPark Seed', '{"source":"mega-rich-seed","theme":"tech"}', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (md5('audit-bcons-rich-seed')::uuid, md5('tenant-bcons')::uuid, md5('user-bcons-manager')::uuid, 'SEED_FACILITY', 'Parking', md5('parking-bcons-plaza')::uuid, '10.30.1.10', 'SmartPark Seed', '{"source":"mega-rich-seed","theme":"residential"}', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO notification (
    id, tenant_id, recipient_user_id, title, content, notification_type, status, read_at
)
VALUES
    (md5('notification-vincom-maintenance')::uuid, md5('tenant-vincom')::uuid, md5('user-vincom-manager')::uuid, 'EV charger zone needs review', 'B2 Zone E is under scheduled maintenance.', 'PARKING', 'UNREAD', NULL),
    (md5('notification-fpt-session-invoice')::uuid, md5('tenant-fpt')::uuid, md5('user-fpt-manager')::uuid, 'Parking invoice issued', 'Invoice FPT-INV-202605-001 is waiting for payment.', 'BILLING', 'UNREAD', NULL),
    (md5('notification-bcons-capacity')::uuid, md5('tenant-bcons')::uuid, md5('user-bcons-manager')::uuid, 'Residential bike area is busy', 'B1 Resident Motorcycles is above normal occupancy.', 'PARKING', 'UNREAD', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO api_traffic_logs (id, method, path, status_code, duration_ms, occurred_at)
VALUES
    (md5('api-traffic-login-vincom')::uuid, 'POST', '/auth/login', 200, 42, CURRENT_TIMESTAMP - INTERVAL '10 minutes'),
    (md5('api-traffic-manager-topology')::uuid, 'GET', '/manager/parkings/{id}/topology', 200, 31, CURRENT_TIMESTAMP - INTERVAL '8 minutes'),
    (md5('api-traffic-manager-slots')::uuid, 'GET', '/manager/slots', 200, 26, CURRENT_TIMESTAMP - INTERVAL '5 minutes'),
    (md5('api-traffic-dashboard')::uuid, 'GET', '/admin/dashboard/stats', 200, 25, CURRENT_TIMESTAMP - INTERVAL '4 minutes')
ON CONFLICT DO NOTHING;
