ALTER TABLE fire_extinguisher_inspections
    ADD COLUMN IF NOT EXISTS photo_object_key TEXT;

CREATE INDEX IF NOT EXISTS idx_fire_extinguisher_inspections_photo_object_key
    ON fire_extinguisher_inspections (photo_object_key)
    WHERE photo_object_key IS NOT NULL;
