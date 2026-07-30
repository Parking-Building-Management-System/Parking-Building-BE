CREATE INDEX IF NOT EXISTS idx_parking_sessions_analytics_check_in
    ON parking_sessions (tenant_id, parking_id, check_in_at)
    INCLUDE (vehicle_type_id, check_out_at);

CREATE INDEX IF NOT EXISTS idx_parking_sessions_analytics_check_out
    ON parking_sessions (tenant_id, parking_id, check_out_at)
    INCLUDE (vehicle_type_id)
    WHERE check_out_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_intents_analytics_paid
    ON payment_intents (tenant_id, paid_at)
    INCLUDE (parking_session_id, amount)
    WHERE is_deleted = false
      AND status = 'PAID'
      AND provider = 'PAYOS';
