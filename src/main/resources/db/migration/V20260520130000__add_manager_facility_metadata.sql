CREATE TABLE floors (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_floors PRIMARY KEY (id),
    CONSTRAINT uk_floors_tenant_parking_code UNIQUE (tenant_id, parking_id, code),
    CONSTRAINT fk_floors_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_floors_parking FOREIGN KEY (parking_id) REFERENCES parkings(id)
);

ALTER TABLE zones
    ADD COLUMN floor_id UUID,
    ADD COLUMN vehicle_type_id UUID;

ALTER TABLE slots
    ADD COLUMN floor_id UUID;

ALTER TABLE zones
    ADD CONSTRAINT fk_zones_floor FOREIGN KEY (floor_id) REFERENCES floors(id),
    ADD CONSTRAINT fk_zones_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id);

ALTER TABLE slots
    ADD CONSTRAINT fk_slots_floor FOREIGN KEY (floor_id) REFERENCES floors(id);

CREATE INDEX idx_floors_tenant_parking ON floors (tenant_id, parking_id, is_deleted);
CREATE INDEX idx_zones_floor_vehicle ON zones (tenant_id, floor_id, vehicle_type_id, is_deleted);
CREATE INDEX idx_slots_floor_status ON slots (tenant_id, floor_id, status, is_deleted);
