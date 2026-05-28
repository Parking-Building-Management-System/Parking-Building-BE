ALTER TABLE floors
    ADD COLUMN IF NOT EXISTS map_image_url VARCHAR(1000);

ALTER TABLE slots
    ADD COLUMN IF NOT EXISTS x_coordinate NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS y_coordinate NUMERIC(5, 2);

ALTER TABLE slots
    ADD CONSTRAINT ck_slots_x_coordinate_percent
        CHECK (x_coordinate IS NULL OR (x_coordinate >= 0 AND x_coordinate <= 100)),
    ADD CONSTRAINT ck_slots_y_coordinate_percent
        CHECK (y_coordinate IS NULL OR (y_coordinate >= 0 AND y_coordinate <= 100));

CREATE INDEX IF NOT EXISTS idx_slots_floor_coordinates
    ON slots (tenant_id, floor_id, is_deleted, code);
