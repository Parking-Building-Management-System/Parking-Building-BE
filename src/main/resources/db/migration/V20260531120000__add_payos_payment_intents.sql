ALTER TABLE parking_sessions
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS exit_deadline TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_session_id UUID NOT NULL,
    rfid_card_id UUID,
    order_code BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'PAYOS',
    provider_payment_link_id VARCHAR(255),
    checkout_url TEXT,
    qr_code TEXT,
    description VARCHAR(120),
    pricing_rule_id UUID,
    quote_snapshot_json TEXT,
    raw_provider_response TEXT,
    paid_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_payment_intents PRIMARY KEY (id),
    CONSTRAINT uk_payment_intents_order_code UNIQUE (order_code),
    CONSTRAINT fk_payment_intents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_payment_intents_parking_session FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_payment_intents_rfid_card FOREIGN KEY (rfid_card_id) REFERENCES rfid_cards(id),
    CONSTRAINT fk_payment_intents_pricing_rule FOREIGN KEY (pricing_rule_id) REFERENCES pricing_rules(id),
    CONSTRAINT ck_payment_intents_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_intents_session
    ON payment_intents (parking_session_id);

CREATE INDEX IF NOT EXISTS idx_payment_intents_tenant
    ON payment_intents (tenant_id);

CREATE INDEX IF NOT EXISTS idx_payment_intents_status
    ON payment_intents (status);

CREATE TABLE IF NOT EXISTS payment_webhook_logs (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    provider VARCHAR(30) NOT NULL,
    event_code VARCHAR(100),
    order_code BIGINT,
    signature TEXT,
    payload_json TEXT NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,

    CONSTRAINT pk_payment_webhook_logs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_order_code
    ON payment_webhook_logs (order_code);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_status
    ON payment_webhook_logs (status);
