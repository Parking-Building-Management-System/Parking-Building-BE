ALTER TABLE kiosk
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'ENTRY',
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE kiosk_staff
    ALTER COLUMN shift_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_kiosk_staff_active_no_shift
    ON kiosk_staff (tenant_id, kiosk_id, staff_user_id)
    WHERE shift_id IS NULL;

ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS kiosk_id UUID;

ALTER TABLE devices
    ADD CONSTRAINT fk_devices_kiosk
    FOREIGN KEY (kiosk_id) REFERENCES kiosk(id);

CREATE INDEX IF NOT EXISTS idx_kiosk_tenant_parking_status
    ON kiosk (tenant_id, parking_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_devices_kiosk_status
    ON devices (kiosk_id, status);
