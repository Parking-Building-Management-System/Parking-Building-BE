CREATE TABLE pricing_rules (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID,
    vehicle_type_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    free_minutes INTEGER NOT NULL DEFAULT 0,
    first_block_minutes INTEGER NOT NULL,
    first_block_price NUMERIC(12, 2) NOT NULL,
    next_block_minutes INTEGER NOT NULL,
    next_block_price NUMERIC(12, 2) NOT NULL,
    daily_cap_price NUMERIC(12, 2),
    grace_minutes_after_payment INTEGER NOT NULL DEFAULT 15,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_pricing_rules PRIMARY KEY (id),
    CONSTRAINT fk_pricing_rules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_pricing_rules_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_pricing_rules_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id),
    CONSTRAINT ck_pricing_rules_free_minutes CHECK (free_minutes >= 0),
    CONSTRAINT ck_pricing_rules_first_block_minutes CHECK (first_block_minutes > 0),
    CONSTRAINT ck_pricing_rules_first_block_price CHECK (first_block_price >= 0),
    CONSTRAINT ck_pricing_rules_next_block_minutes CHECK (next_block_minutes > 0),
    CONSTRAINT ck_pricing_rules_next_block_price CHECK (next_block_price >= 0),
    CONSTRAINT ck_pricing_rules_daily_cap_price CHECK (daily_cap_price IS NULL OR daily_cap_price >= 0),
    CONSTRAINT ck_pricing_rules_grace_minutes CHECK (grace_minutes_after_payment >= 0)
);

CREATE INDEX idx_pricing_rules_tenant
    ON pricing_rules (tenant_id);

CREATE INDEX idx_pricing_rules_scope
    ON pricing_rules (tenant_id, parking_id, vehicle_type_id, status, is_deleted);

CREATE UNIQUE INDEX uk_pricing_rules_active_scope
    ON pricing_rules (
        tenant_id,
        COALESCE(parking_id, '00000000-0000-0000-0000-000000000000'::uuid),
        vehicle_type_id
    )
    WHERE status = 'ACTIVE' AND is_deleted = false;

WITH pricing_seed(
    id_key,
    tenant_key,
    parking_key,
    vehicle_type_key,
    name,
    free_minutes,
    first_block_minutes,
    first_block_price,
    next_block_minutes,
    next_block_price,
    daily_cap_price,
    grace_minutes_after_payment
) AS (
    VALUES
        ('pricing-vincom-dk-car', 'tenant-vincom', 'parking-vincom-dongkhoi', 'vehicle-type-car', 'Vincom Dong Khoi Car Standard', 10, 120, 20000.00, 60, 10000.00, 100000.00, 15),
        ('pricing-vincom-dk-ev', 'tenant-vincom', 'parking-vincom-dongkhoi', 'vehicle-type-electric-car', 'Vincom Dong Khoi EV Standard', 10, 120, 25000.00, 60, 12000.00, 120000.00, 15),
        ('pricing-vincom-dk-bike', 'tenant-vincom', 'parking-vincom-dongkhoi', 'vehicle-type-motorbike', 'Vincom Dong Khoi Motorbike Standard', 10, 240, 5000.00, 120, 5000.00, 30000.00, 15),
        ('pricing-vincom-td-car', 'tenant-vincom', 'parking-vincom-thaodien', 'vehicle-type-car', 'Vincom Thao Dien Car Standard', 10, 120, 20000.00, 60, 10000.00, 100000.00, 15),
        ('pricing-vincom-td-ev', 'tenant-vincom', 'parking-vincom-thaodien', 'vehicle-type-electric-car', 'Vincom Thao Dien EV Standard', 10, 120, 25000.00, 60, 12000.00, 120000.00, 15),
        ('pricing-vincom-td-bike', 'tenant-vincom', 'parking-vincom-thaodien', 'vehicle-type-motorbike', 'Vincom Thao Dien Motorbike Standard', 10, 240, 5000.00, 120, 5000.00, 30000.00, 15),
        ('pricing-fpt-car', 'tenant-fpt', 'parking-fpt-tower', 'vehicle-type-car', 'FPT Tower Car Standard', 10, 120, 20000.00, 60, 10000.00, 100000.00, 15),
        ('pricing-fpt-ev', 'tenant-fpt', 'parking-fpt-tower', 'vehicle-type-electric-car', 'FPT Tower EV Standard', 10, 120, 25000.00, 60, 12000.00, 120000.00, 15),
        ('pricing-fpt-bike', 'tenant-fpt', 'parking-fpt-tower', 'vehicle-type-motorbike', 'FPT Tower Motorbike Standard', 10, 240, 5000.00, 120, 5000.00, 30000.00, 15),
        ('pricing-bcons-car', 'tenant-bcons', 'parking-bcons-plaza', 'vehicle-type-car', 'Bcons Plaza Car Standard', 10, 120, 20000.00, 60, 10000.00, 100000.00, 15),
        ('pricing-bcons-ev', 'tenant-bcons', 'parking-bcons-plaza', 'vehicle-type-electric-car', 'Bcons Plaza EV Standard', 10, 120, 25000.00, 60, 12000.00, 120000.00, 15),
        ('pricing-bcons-bike', 'tenant-bcons', 'parking-bcons-plaza', 'vehicle-type-motorbike', 'Bcons Plaza Motorbike Standard', 10, 240, 5000.00, 120, 5000.00, 30000.00, 15)
)
INSERT INTO pricing_rules (
    id,
    tenant_id,
    parking_id,
    vehicle_type_id,
    name,
    free_minutes,
    first_block_minutes,
    first_block_price,
    next_block_minutes,
    next_block_price,
    daily_cap_price,
    grace_minutes_after_payment,
    status,
    is_deleted
)
SELECT
    md5(id_key)::uuid,
    md5(tenant_key)::uuid,
    md5(parking_key)::uuid,
    md5(vehicle_type_key)::uuid,
    name,
    free_minutes,
    first_block_minutes,
    first_block_price,
    next_block_minutes,
    next_block_price,
    daily_cap_price,
    grace_minutes_after_payment,
    'ACTIVE',
    false
FROM pricing_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM pricing_rules existing
    WHERE existing.tenant_id = md5(seed.tenant_key)::uuid
      AND existing.parking_id = md5(seed.parking_key)::uuid
      AND existing.vehicle_type_id = md5(seed.vehicle_type_key)::uuid
      AND existing.status = 'ACTIVE'
      AND existing.is_deleted = false
);
