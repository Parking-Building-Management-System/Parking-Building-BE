ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE permissions
SET
    scope = COALESCE(scope, 'GLOBAL'),
    module = COALESCE(module, 'GENERAL'),
    resource = COALESCE(resource, name),
    label = COALESCE(label, name),
    action = COALESCE(action, 'ACCESS'),
    status = COALESCE(status, 'ACTIVE'),
    is_deleted = COALESCE(is_deleted, FALSE);

CREATE INDEX IF NOT EXISTS idx_permissions_tree
    ON permissions (is_deleted, status, scope, module, resource, label, action);

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(100),
    ADD COLUMN IF NOT EXISTS severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    ADD COLUMN IF NOT EXISTS reason TEXT,
    ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);

UPDATE audit_logs
SET severity = COALESCE(severity, 'INFO');

CREATE INDEX IF NOT EXISTS idx_audit_logs_admin_search
    ON audit_logs (occurred_at DESC, severity, actor_role);

CREATE INDEX IF NOT EXISTS idx_sessions_admin_search
    ON sessions (revoked_at, expired_at, created_at DESC);
