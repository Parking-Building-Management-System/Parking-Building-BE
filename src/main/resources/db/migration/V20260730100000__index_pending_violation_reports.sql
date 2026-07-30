CREATE INDEX idx_penalty_cases_pending_pwa_violations
    ON parking_penalty_cases (tenant_id, parking_id)
    WHERE type = 'OCCUPIED_ASSIGNED_SLOT'
      AND reported_from_pwa = true
      AND status = 'REPORTED';
