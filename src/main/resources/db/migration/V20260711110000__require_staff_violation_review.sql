ALTER TABLE parking_penalty_cases DROP CONSTRAINT ck_penalty_cases_status;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT ck_penalty_cases_status
        CHECK (status IN ('REPORTED', 'APPLIED', 'REJECTED', 'WAIVED', 'COLLECTED'));

ALTER TABLE parking_penalty_cases
    ADD COLUMN reviewed_by_staff_id UUID,
    ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN review_note VARCHAR(1000),
    ADD CONSTRAINT fk_penalty_cases_reviewed_by_staff
        FOREIGN KEY (reviewed_by_staff_id) REFERENCES users(id);

CREATE INDEX idx_penalty_cases_parking_status_created
    ON parking_penalty_cases (tenant_id, parking_id, status, created_at DESC);
CREATE INDEX idx_penalty_cases_offender_session_status
    ON parking_penalty_cases (tenant_id, offender_session_id, status);
CREATE INDEX idx_penalty_cases_victim_session
    ON parking_penalty_cases (tenant_id, victim_session_id);
