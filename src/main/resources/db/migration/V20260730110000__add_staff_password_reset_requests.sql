ALTER TABLE users
    ADD CONSTRAINT uk_users_tenant_id_id UNIQUE (tenant_id, id);

CREATE TABLE staff_password_reset_requests (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    tenant_id UUID NOT NULL,
    staff_user_id UUID NOT NULL,
    requested_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by_manager_id UUID,
    completed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(1000),

    CONSTRAINT pk_staff_password_reset_requests PRIMARY KEY (id),
    CONSTRAINT fk_staff_password_reset_requests_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_staff_password_reset_requests_staff
        FOREIGN KEY (tenant_id, staff_user_id) REFERENCES users(tenant_id, id),
    CONSTRAINT fk_staff_password_reset_requests_manager
        FOREIGN KEY (tenant_id, reviewed_by_manager_id) REFERENCES users(tenant_id, id),
    CONSTRAINT ck_staff_password_reset_requests_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED')),
    CONSTRAINT ck_staff_password_reset_requests_outcome
        CHECK (
            (
                status = 'PENDING'
                AND reviewed_at IS NULL
                AND reviewed_by_manager_id IS NULL
                AND completed_at IS NULL
                AND rejection_reason IS NULL
            )
            OR (
                status = 'COMPLETED'
                AND reviewed_at IS NOT NULL
                AND reviewed_by_manager_id IS NOT NULL
                AND completed_at IS NOT NULL
                AND rejection_reason IS NULL
            )
            OR (
                status = 'REJECTED'
                AND reviewed_at IS NOT NULL
                AND reviewed_by_manager_id IS NOT NULL
                AND completed_at IS NULL
                AND rejection_reason IS NOT NULL
            )
        )
);

CREATE INDEX idx_staff_password_reset_requests_tenant_status_requested
    ON staff_password_reset_requests (tenant_id, status, requested_at DESC);

CREATE UNIQUE INDEX uk_staff_password_reset_requests_pending_staff
    ON staff_password_reset_requests (staff_user_id)
    WHERE status = 'PENDING';

CREATE FUNCTION enforce_staff_password_reset_request_roles()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM users staff
        JOIN user_roles staff_user_role ON staff_user_role.user_id = staff.id
        JOIN roles staff_role ON staff_role.id = staff_user_role.role_id
        WHERE staff.id = NEW.staff_user_id
          AND staff.tenant_id = NEW.tenant_id
          AND staff_role.name = 'STAFF'
    ) THEN
        RAISE EXCEPTION 'staff_password_reset_requests requires a STAFF user'
            USING ERRCODE = '23514';
    END IF;

    IF NEW.reviewed_by_manager_id IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM users manager
        JOIN user_roles manager_user_role ON manager_user_role.user_id = manager.id
        JOIN roles manager_role ON manager_role.id = manager_user_role.role_id
        WHERE manager.id = NEW.reviewed_by_manager_id
          AND manager.tenant_id = NEW.tenant_id
          AND manager_role.name = 'PARKING_MANAGER'
    ) THEN
        RAISE EXCEPTION 'staff_password_reset_requests reviewer requires a PARKING_MANAGER user'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_staff_password_reset_request_roles
    BEFORE INSERT OR UPDATE ON staff_password_reset_requests
    FOR EACH ROW
    EXECUTE FUNCTION enforce_staff_password_reset_request_roles();
