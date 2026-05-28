ALTER TABLE rfid_cards
    ADD COLUMN IF NOT EXISTS qr_token VARCHAR(120);

UPDATE rfid_cards
SET qr_token = 'qr_' || md5(id::text || ':' || uid || ':' || created_at::text)
WHERE qr_token IS NULL;

ALTER TABLE rfid_cards
    ALTER COLUMN qr_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_rfid_cards_qr_token
    ON rfid_cards (qr_token);
