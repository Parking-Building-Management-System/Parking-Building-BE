CREATE TABLE parking_penalty_rules (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    requires_photo BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_parking_penalty_rules PRIMARY KEY (id),
    CONSTRAINT fk_penalty_rules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_penalty_rules_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT ck_penalty_rules_amount CHECK (amount >= 0),
    CONSTRAINT ck_penalty_rules_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_penalty_rules_type CHECK (
        type IN ('OCCUPIED_ASSIGNED_SLOT', 'ILLEGAL_PARKING', 'LOST_CARD', 'BLOCKING_LANE', 'OTHER')
    )
);

CREATE INDEX idx_penalty_rules_tenant
    ON parking_penalty_rules (tenant_id);

CREATE INDEX idx_penalty_rules_scope
    ON parking_penalty_rules (tenant_id, parking_id, type, status, is_deleted);

CREATE UNIQUE INDEX uk_penalty_rules_active_scope
    ON parking_penalty_rules (
        tenant_id,
        COALESCE(parking_id, '00000000-0000-0000-0000-000000000000'::uuid),
        type
    )
    WHERE status = 'ACTIVE' AND is_deleted = false;

CREATE TABLE parking_penalty_cases (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    rule_id UUID,
    type VARCHAR(40) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    target_session_id UUID,
    victim_session_id UUID,
    offender_session_id UUID,
    reported_slot_id UUID,
    reassigned_slot_id UUID,
    target_license_plate VARCHAR(30),
    offender_license_plate VARCHAR(30),
    evidence_image_url VARCHAR(1000),
    identity_image_url VARCHAR(1000),
    vehicle_image_url VARCHAR(1000),
    license_plate_image_url VARCHAR(1000),
    reported_from_pwa BOOLEAN NOT NULL DEFAULT FALSE,
    reported_by_staff_id UUID,
    note VARCHAR(1000),
    resolved_at TIMESTAMP WITH TIME ZONE,
    collected_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_parking_penalty_cases PRIMARY KEY (id),
    CONSTRAINT fk_penalty_cases_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_penalty_cases_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_penalty_cases_rule FOREIGN KEY (rule_id) REFERENCES parking_penalty_rules(id),
    CONSTRAINT fk_penalty_cases_target_session FOREIGN KEY (target_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_penalty_cases_victim_session FOREIGN KEY (victim_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_penalty_cases_offender_session FOREIGN KEY (offender_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_penalty_cases_reported_slot FOREIGN KEY (reported_slot_id) REFERENCES slots(id),
    CONSTRAINT fk_penalty_cases_reassigned_slot FOREIGN KEY (reassigned_slot_id) REFERENCES slots(id),
    CONSTRAINT fk_penalty_cases_staff FOREIGN KEY (reported_by_staff_id) REFERENCES users(id),
    CONSTRAINT ck_penalty_cases_amount CHECK (amount >= 0),
    CONSTRAINT ck_penalty_cases_status CHECK (status IN ('REPORTED', 'APPLIED', 'WAIVED', 'COLLECTED')),
    CONSTRAINT ck_penalty_cases_type CHECK (
        type IN ('OCCUPIED_ASSIGNED_SLOT', 'ILLEGAL_PARKING', 'LOST_CARD', 'BLOCKING_LANE', 'OTHER')
    )
);

CREATE INDEX idx_penalty_cases_target_session_status
    ON parking_penalty_cases (tenant_id, target_session_id, status);

CREATE INDEX idx_penalty_cases_parking_created
    ON parking_penalty_cases (tenant_id, parking_id, created_at DESC);

WITH default_penalties(type, code, name, amount, requires_photo, description) AS (
    VALUES
        (
            'OCCUPIED_ASSIGNED_SLOT',
            'OCCUPIED_ASSIGNED_SLOT',
            'Slot occupied by another vehicle',
            50000.00,
            TRUE,
            'Fine applied when another active vehicle occupies a driver assigned slot.'
        ),
        (
            'LOST_CARD',
            'LOST_CARD',
            'Lost RFID card',
            100000.00,
            TRUE,
            'Fine applied when a driver reports a lost RFID card at exit.'
        )
)
INSERT INTO parking_penalty_rules (
    id,
    tenant_id,
    parking_id,
    code,
    name,
    type,
    amount,
    currency,
    requires_photo,
    description,
    status,
    is_deleted
)
SELECT
    md5('penalty-rule-' || t.id::text || '-' || dp.type)::uuid,
    t.id,
    NULL,
    dp.code,
    dp.name,
    dp.type,
    dp.amount,
    'VND',
    dp.requires_photo,
    dp.description,
    'ACTIVE',
    FALSE
FROM tenants t
CROSS JOIN default_penalties dp
WHERE NOT EXISTS (
    SELECT 1
    FROM parking_penalty_rules existing
    WHERE existing.tenant_id = t.id
      AND existing.parking_id IS NULL
      AND existing.type = dp.type
      AND existing.status = 'ACTIVE'
      AND existing.is_deleted = false
);
