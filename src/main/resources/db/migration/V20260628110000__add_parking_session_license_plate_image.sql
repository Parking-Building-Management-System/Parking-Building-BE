ALTER TABLE parking_sessions
    ADD COLUMN IF NOT EXISTS license_plate_image_url VARCHAR(1000);
