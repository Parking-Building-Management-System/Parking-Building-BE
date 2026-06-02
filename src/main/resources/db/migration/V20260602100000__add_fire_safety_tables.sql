CREATE TABLE fire_extinguishers (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    floor_id UUID NOT NULL,
    zone_id UUID,
    code VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    location_description TEXT,
    x_coordinate NUMERIC(5, 2),
    y_coordinate NUMERIC(5, 2),
    manufacture_date DATE,
    expiry_date DATE,
    last_inspected_at TIMESTAMP WITH TIME ZONE,
    next_inspection_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_fire_extinguishers PRIMARY KEY (id),
    CONSTRAINT fk_fire_extinguishers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_fire_extinguishers_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_fire_extinguishers_floor FOREIGN KEY (floor_id) REFERENCES floors(id),
    CONSTRAINT fk_fire_extinguishers_zone FOREIGN KEY (zone_id) REFERENCES zones(id),
    CONSTRAINT ck_fire_extinguishers_x_coordinate_percent
        CHECK (x_coordinate IS NULL OR (x_coordinate >= 0 AND x_coordinate <= 100)),
    CONSTRAINT ck_fire_extinguishers_y_coordinate_percent
        CHECK (y_coordinate IS NULL OR (y_coordinate >= 0 AND y_coordinate <= 100))
);

CREATE UNIQUE INDEX uk_fire_extinguishers_tenant_code_active
    ON fire_extinguishers (tenant_id, lower(code))
    WHERE is_deleted = FALSE;

CREATE INDEX idx_fire_extinguishers_tenant
    ON fire_extinguishers (tenant_id);

CREATE INDEX idx_fire_extinguishers_parking
    ON fire_extinguishers (parking_id);

CREATE INDEX idx_fire_extinguishers_floor
    ON fire_extinguishers (floor_id);

CREATE INDEX idx_fire_extinguishers_zone
    ON fire_extinguishers (zone_id);

CREATE INDEX idx_fire_extinguishers_status
    ON fire_extinguishers (status);

CREATE INDEX idx_fire_extinguishers_expiry_date
    ON fire_extinguishers (expiry_date);

CREATE INDEX idx_fire_extinguishers_next_inspection_at
    ON fire_extinguishers (next_inspection_at);

CREATE INDEX idx_fire_extinguishers_is_deleted
    ON fire_extinguishers (is_deleted);

CREATE TABLE fire_extinguisher_inspections (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    fire_extinguisher_id UUID NOT NULL,
    inspected_by UUID,
    result VARCHAR(30) NOT NULL,
    pressure_ok BOOLEAN,
    seal_ok BOOLEAN,
    location_ok BOOLEAN,
    expiry_ok BOOLEAN,
    photo_url TEXT,
    note TEXT,
    inspected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_inspection_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_fire_extinguisher_inspections PRIMARY KEY (id),
    CONSTRAINT fk_fire_extinguisher_inspections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_fire_extinguisher_inspections_extinguisher
        FOREIGN KEY (fire_extinguisher_id) REFERENCES fire_extinguishers(id),
    CONSTRAINT fk_fire_extinguisher_inspections_user FOREIGN KEY (inspected_by) REFERENCES users(id)
);

CREATE INDEX idx_fire_extinguisher_inspections_tenant
    ON fire_extinguisher_inspections (tenant_id);

CREATE INDEX idx_fire_extinguisher_inspections_extinguisher
    ON fire_extinguisher_inspections (fire_extinguisher_id);

CREATE INDEX idx_fire_extinguisher_inspections_inspected_by
    ON fire_extinguisher_inspections (inspected_by);

CREATE INDEX idx_fire_extinguisher_inspections_result
    ON fire_extinguisher_inspections (result);

CREATE INDEX idx_fire_extinguisher_inspections_inspected_at
    ON fire_extinguisher_inspections (inspected_at);

WITH floor_zone AS (
    SELECT
        f.tenant_id,
        f.parking_id,
        f.id AS floor_id,
        f.code AS floor_code,
        p.code AS parking_code,
        (
            SELECT z.id
            FROM zones z
            WHERE z.tenant_id = f.tenant_id
              AND z.floor_id = f.id
              AND z.is_deleted = FALSE
            ORDER BY z.name ASC
            LIMIT 1
        ) AS zone_id
    FROM floors f
    JOIN parkings p ON p.id = f.parking_id
    WHERE f.is_deleted = FALSE
      AND p.is_deleted = FALSE
),
demo_extinguishers(seq, type, x_coordinate, y_coordinate, expiry_offset_days, next_inspection_offset_days, status, location_description) AS (
    VALUES
        (1, 'CO2', 12.50, 14.00, 730, -7, 'ACTIVE', 'Near main entry'),
        (2, 'POWDER', 88.00, 16.50, 45, 10, 'ACTIVE', 'Near elevator lobby'),
        (3, 'FOAM', 18.00, 84.00, -20, -14, 'EXPIRED', 'Back corner wall'),
        (4, 'CO2', 82.00, 78.00, 540, 30, 'ACTIVE', 'Near ramp exit'),
        (5, 'POWDER', 50.00, 48.00, 365, -2, 'MAINTENANCE', 'Central column'),
        (6, 'FOAM', 64.00, 28.00, 680, 21, 'ACTIVE', 'Service corridor')
)
INSERT INTO fire_extinguishers (
    id,
    tenant_id,
    parking_id,
    floor_id,
    zone_id,
    code,
    type,
    location_description,
    x_coordinate,
    y_coordinate,
    manufacture_date,
    expiry_date,
    last_inspected_at,
    next_inspection_at,
    status,
    note,
    is_deleted
)
SELECT
    md5('fire-extinguisher:' || fz.tenant_id || ':' || fz.floor_id || ':' || de.seq)::uuid,
    fz.tenant_id,
    fz.parking_id,
    fz.floor_id,
    fz.zone_id,
    'FE-' || regexp_replace(fz.parking_code, '[^A-Za-z0-9]+', '', 'g') || '-' || fz.floor_code || '-' || lpad(de.seq::text, 3, '0'),
    de.type,
    de.location_description,
    de.x_coordinate,
    de.y_coordinate,
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + (de.expiry_offset_days || ' days')::interval,
    CASE WHEN de.seq IN (1, 3, 5) THEN CURRENT_TIMESTAMP - INTERVAL '35 days' ELSE NULL END,
    CURRENT_TIMESTAMP + (de.next_inspection_offset_days || ' days')::interval,
    de.status,
    'Demo fire safety seed',
    FALSE
FROM floor_zone fz
CROSS JOIN demo_extinguishers de
ON CONFLICT (tenant_id, lower(code)) WHERE is_deleted = FALSE DO NOTHING;

WITH seeded AS (
    SELECT fe.*
    FROM fire_extinguishers fe
    WHERE fe.note = 'Demo fire safety seed'
      AND fe.is_deleted = FALSE
      AND right(fe.code, 3) IN ('001', '003', '005')
)
INSERT INTO fire_extinguisher_inspections (
    id,
    tenant_id,
    fire_extinguisher_id,
    inspected_by,
    result,
    pressure_ok,
    seal_ok,
    location_ok,
    expiry_ok,
    photo_url,
    note,
    inspected_at,
    next_inspection_at
)
SELECT
    md5('fire-extinguisher-inspection:' || s.id)::uuid,
    s.tenant_id,
    s.id,
    NULL,
    CASE s.status WHEN 'EXPIRED' THEN 'EXPIRED' WHEN 'MAINTENANCE' THEN 'NEEDS_REPLACEMENT' ELSE 'OK' END,
    s.status <> 'MAINTENANCE',
    TRUE,
    TRUE,
    s.status <> 'EXPIRED',
    NULL,
    'Demo inspection log',
    COALESCE(s.last_inspected_at, CURRENT_TIMESTAMP - INTERVAL '35 days'),
    s.next_inspection_at
FROM seeded s
ON CONFLICT (id) DO NOTHING;
