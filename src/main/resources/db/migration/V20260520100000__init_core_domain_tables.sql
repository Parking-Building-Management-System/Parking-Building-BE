CREATE TABLE vehicle_types (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_vehicle_types PRIMARY KEY (id),
    CONSTRAINT uk_vehicle_types_code UNIQUE (code)
);

CREATE TABLE parkings (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    total_capacity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_parkings PRIMARY KEY (id),
    CONSTRAINT uk_parkings_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_parkings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE zones (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    floor_name VARCHAR(50),
    capacity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_zones PRIMARY KEY (id),
    CONSTRAINT uk_zones_tenant_parking_code UNIQUE (tenant_id, parking_id, code),
    CONSTRAINT fk_zones_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_zones_parking FOREIGN KEY (parking_id) REFERENCES parkings(id)
);

CREATE TABLE slots (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    zone_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    slot_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_slots PRIMARY KEY (id),
    CONSTRAINT uk_slots_tenant_zone_code UNIQUE (tenant_id, zone_id, code),
    CONSTRAINT fk_slots_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_slots_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_slots_zone FOREIGN KEY (zone_id) REFERENCES zones(id)
);

CREATE TABLE rfid_cards (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    uid VARCHAR(100) NOT NULL,
    assigned_user_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    activated_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_rfid_cards PRIMARY KEY (id),
    CONSTRAINT uk_rfid_cards_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT uk_rfid_cards_tenant_uid UNIQUE (tenant_id, uid),
    CONSTRAINT fk_rfid_cards_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_rfid_cards_user FOREIGN KEY (assigned_user_id) REFERENCES users(id)
);

CREATE TABLE user_vehicle_link (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vehicle_type_id UUID NOT NULL,
    license_plate VARCHAR(30) NOT NULL,
    vehicle_label VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_user_vehicle_link PRIMARY KEY (id),
    CONSTRAINT uk_user_vehicle_link_plate UNIQUE (tenant_id, license_plate),
    CONSTRAINT fk_user_vehicle_link_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_user_vehicle_link_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_vehicle_link_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id)
);

CREATE TABLE shift (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_shift PRIMARY KEY (id),
    CONSTRAINT uk_shift_tenant_parking_name UNIQUE (tenant_id, parking_id, name),
    CONSTRAINT fk_shift_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_shift_parking FOREIGN KEY (parking_id) REFERENCES parkings(id)
);

CREATE TABLE kiosk (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_kiosk PRIMARY KEY (id),
    CONSTRAINT uk_kiosk_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_kiosk_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_kiosk_parking FOREIGN KEY (parking_id) REFERENCES parkings(id)
);

CREATE TABLE kiosk_staff (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    kiosk_id UUID NOT NULL,
    staff_user_id UUID NOT NULL,
    shift_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_kiosk_staff PRIMARY KEY (id),
    CONSTRAINT uk_kiosk_staff_active UNIQUE (tenant_id, kiosk_id, staff_user_id, shift_id),
    CONSTRAINT fk_kiosk_staff_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_kiosk_staff_kiosk FOREIGN KEY (kiosk_id) REFERENCES kiosk(id),
    CONSTRAINT fk_kiosk_staff_user FOREIGN KEY (staff_user_id) REFERENCES users(id),
    CONSTRAINT fk_kiosk_staff_shift FOREIGN KEY (shift_id) REFERENCES shift(id)
);

CREATE TABLE parking_sessions (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    zone_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    rfid_card_id UUID,
    vehicle_type_id UUID NOT NULL,
    user_vehicle_link_id UUID,
    license_plate VARCHAR(30) NOT NULL,
    check_in_at TIMESTAMP WITH TIME ZONE NOT NULL,
    check_out_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    entry_image_url VARCHAR(1000),
    exit_image_url VARCHAR(1000),
    total_amount NUMERIC(12, 2),

    CONSTRAINT pk_parking_sessions PRIMARY KEY (id),
    CONSTRAINT fk_ps_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ps_parking FOREIGN KEY (parking_id) REFERENCES parkings(id),
    CONSTRAINT fk_ps_zone FOREIGN KEY (zone_id) REFERENCES zones(id),
    CONSTRAINT fk_ps_slot FOREIGN KEY (slot_id) REFERENCES slots(id),
    CONSTRAINT fk_ps_rfid FOREIGN KEY (rfid_card_id) REFERENCES rfid_cards(id),
    CONSTRAINT fk_ps_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types(id),
    CONSTRAINT fk_ps_vehicle_link FOREIGN KEY (user_vehicle_link_id) REFERENCES user_vehicle_link(id)
);

CREATE TABLE subscriptions (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_vehicle_link_id UUID NOT NULL,
    parking_id UUID NOT NULL,
    period_string VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_price NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_subscriptions_vehicle FOREIGN KEY (user_vehicle_link_id) REFERENCES user_vehicle_link(id),
    CONSTRAINT fk_subscriptions_parking FOREIGN KEY (parking_id) REFERENCES parkings(id)
);

CREATE TABLE invoice (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    invoice_no VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    subscription_id UUID,
    parking_session_id UUID,
    invoice_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    amount NUMERIC(12, 2) NOT NULL,
    tax_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_invoice PRIMARY KEY (id),
    CONSTRAINT uk_invoice_tenant_no UNIQUE (tenant_id, invoice_no),
    CONSTRAINT fk_invoice_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_invoice_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_invoice_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT fk_invoice_parking_session FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id)
);

CREATE TABLE webhook_log (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    external_id VARCHAR(255),
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,

    CONSTRAINT pk_webhook_log PRIMARY KEY (id),
    CONSTRAINT fk_webhook_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE audit_logs (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    actor_user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    metadata TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE TABLE zone_violation_report (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    parking_session_id UUID NOT NULL,
    zone_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    reported_by UUID,
    violation_type VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_zone_violation_report PRIMARY KEY (id),
    CONSTRAINT fk_zvr_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_zvr_session FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id),
    CONSTRAINT fk_zvr_zone FOREIGN KEY (zone_id) REFERENCES zones(id),
    CONSTRAINT fk_zvr_slot FOREIGN KEY (slot_id) REFERENCES slots(id),
    CONSTRAINT fk_zvr_reported_by FOREIGN KEY (reported_by) REFERENCES users(id)
);

CREATE TABLE notification (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    read_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id)
);

CREATE TABLE api_traffic_logs (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    status_code INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_api_traffic_logs PRIMARY KEY (id)
);

CREATE INDEX idx_parkings_tenant_status ON parkings (tenant_id, status, is_deleted);
CREATE INDEX idx_zones_tenant_parking ON zones (tenant_id, parking_id, status, is_deleted);
CREATE INDEX idx_slots_tenant_zone_status ON slots (tenant_id, zone_id, status, is_deleted);
CREATE INDEX idx_rfid_cards_tenant_status ON rfid_cards (tenant_id, status);
CREATE INDEX idx_user_vehicle_link_user ON user_vehicle_link (tenant_id, user_id, is_active);
CREATE INDEX idx_parking_sessions_tenant_status ON parking_sessions (tenant_id, status, check_in_at);
CREATE INDEX idx_subscriptions_tenant_status ON subscriptions (tenant_id, status, end_date);
CREATE INDEX idx_invoice_tenant_status ON invoice (tenant_id, status, issued_at);
CREATE INDEX idx_notification_recipient_status ON notification (tenant_id, recipient_user_id, status);
CREATE INDEX idx_api_traffic_logs_occurred_at ON api_traffic_logs (occurred_at);
