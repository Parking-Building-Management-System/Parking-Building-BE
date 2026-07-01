CREATE TABLE staff_cash_shifts (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    kiosk_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    expected_cash_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    counted_cash_amount NUMERIC(12, 2),
    variance_amount NUMERIC(12, 2),
    online_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cash_parking_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    surcharge_cash_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    penalty_cash_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    lost_card_cash_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    note VARCHAR(1000),

    CONSTRAINT pk_staff_cash_shifts PRIMARY KEY (id),
    CONSTRAINT fk_staff_cash_shifts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_staff_cash_shifts_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_staff_cash_shifts_kiosk FOREIGN KEY (kiosk_id) REFERENCES kiosk(id),
    CONSTRAINT fk_staff_cash_shifts_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT ck_staff_cash_shifts_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_staff_cash_shifts_expected_amount CHECK (expected_cash_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_counted_amount CHECK (counted_cash_amount IS NULL OR counted_cash_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_online_amount CHECK (online_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_cash_parking_amount CHECK (cash_parking_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_surcharge_cash_amount CHECK (surcharge_cash_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_penalty_cash_amount CHECK (penalty_cash_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_lost_card_cash_amount CHECK (lost_card_cash_amount >= 0),
    CONSTRAINT ck_staff_cash_shifts_transaction_count CHECK (transaction_count >= 0)
);

CREATE UNIQUE INDEX uk_staff_cash_shifts_one_open_per_staff
    ON staff_cash_shifts (tenant_id, staff_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_staff_cash_shifts_tenant_status
    ON staff_cash_shifts (tenant_id, status, opened_at DESC);

CREATE INDEX idx_staff_cash_shifts_parking_opened
    ON staff_cash_shifts (tenant_id, parking_id, opened_at DESC);

CREATE TABLE staff_cash_transactions (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    shift_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    kiosk_id UUID NOT NULL,
    staff_id UUID NOT NULL,
    parking_session_id UUID,
    penalty_case_id UUID,
    type VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(30) NOT NULL,
    note VARCHAR(1000),

    CONSTRAINT pk_staff_cash_transactions PRIMARY KEY (id),
    CONSTRAINT fk_staff_cash_transactions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_staff_cash_transactions_shift FOREIGN KEY (shift_id) REFERENCES staff_cash_shifts(id),
    CONSTRAINT fk_staff_cash_transactions_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_staff_cash_transactions_kiosk FOREIGN KEY (kiosk_id) REFERENCES kiosk(id),
    CONSTRAINT fk_staff_cash_transactions_staff FOREIGN KEY (staff_id) REFERENCES users(id),
    CONSTRAINT fk_staff_cash_transactions_session FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_staff_cash_transactions_penalty_case FOREIGN KEY (penalty_case_id) REFERENCES parking_penalty_cases(id),
    CONSTRAINT ck_staff_cash_transactions_amount CHECK (amount >= 0),
    CONSTRAINT ck_staff_cash_transactions_type CHECK (
        type IN ('PARKING_CASH', 'SURCHARGE_CASH', 'PENALTY_CASH', 'LOST_CARD_FINE', 'ADJUSTMENT')
    ),
    CONSTRAINT ck_staff_cash_transactions_source CHECK (
        source IN ('NORMAL_EXIT', 'LOST_CARD_EXIT', 'PENALTY_COLLECTION')
    )
);

CREATE INDEX idx_staff_cash_transactions_shift
    ON staff_cash_transactions (shift_id);

CREATE INDEX idx_staff_cash_transactions_staff_occurred
    ON staff_cash_transactions (staff_id, occurred_at DESC);

CREATE INDEX idx_staff_cash_transactions_parking_occurred
    ON staff_cash_transactions (parking_id, occurred_at DESC);
