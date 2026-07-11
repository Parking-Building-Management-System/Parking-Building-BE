-- Facility resources now use physical deletion. Existing logical deletions are purged as part
-- of this one-time migration; no application startup task removes legacy data.

ALTER TABLE devices DROP CONSTRAINT fk_devices_kiosk;
ALTER TABLE devices
    ADD CONSTRAINT fk_devices_kiosk
        FOREIGN KEY (kiosk_id) REFERENCES kiosk(id) ON DELETE SET NULL;

ALTER TABLE floors DROP CONSTRAINT fk_floors_parking;
ALTER TABLE floors
    ADD CONSTRAINT fk_floors_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE zones DROP CONSTRAINT fk_zones_parking;
ALTER TABLE zones DROP CONSTRAINT fk_zones_floor;
ALTER TABLE zones
    ADD CONSTRAINT fk_zones_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE zones
    ADD CONSTRAINT fk_zones_floor
        FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE;

ALTER TABLE slots DROP CONSTRAINT fk_slots_parking;
ALTER TABLE slots DROP CONSTRAINT fk_slots_zone;
ALTER TABLE slots DROP CONSTRAINT fk_slots_floor;
ALTER TABLE slots
    ADD CONSTRAINT fk_slots_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE slots
    ADD CONSTRAINT fk_slots_zone
        FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE;
ALTER TABLE slots
    ADD CONSTRAINT fk_slots_floor
        FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE;

ALTER TABLE shift DROP CONSTRAINT fk_shift_parking;
ALTER TABLE shift
    ADD CONSTRAINT fk_shift_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE kiosk DROP CONSTRAINT fk_kiosk_parking;
ALTER TABLE kiosk
    ADD CONSTRAINT fk_kiosk_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE kiosk_staff DROP CONSTRAINT fk_kiosk_staff_kiosk;
ALTER TABLE kiosk_staff DROP CONSTRAINT fk_kiosk_staff_shift;
ALTER TABLE kiosk_staff
    ADD CONSTRAINT fk_kiosk_staff_kiosk
        FOREIGN KEY (kiosk_id) REFERENCES kiosk(id) ON DELETE CASCADE;
ALTER TABLE kiosk_staff
    ADD CONSTRAINT fk_kiosk_staff_shift
        FOREIGN KEY (shift_id) REFERENCES shift(id) ON DELETE CASCADE;

ALTER TABLE parking_sessions DROP CONSTRAINT fk_ps_parking;
ALTER TABLE parking_sessions DROP CONSTRAINT fk_ps_zone;
ALTER TABLE parking_sessions DROP CONSTRAINT fk_ps_slot;
ALTER TABLE parking_sessions
    ADD CONSTRAINT fk_ps_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE parking_sessions
    ADD CONSTRAINT fk_ps_zone
        FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE;
ALTER TABLE parking_sessions
    ADD CONSTRAINT fk_ps_slot
        FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE CASCADE;

ALTER TABLE subscriptions DROP CONSTRAINT fk_subscriptions_parking;
ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscriptions_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE invoice DROP CONSTRAINT fk_invoice_subscription;
ALTER TABLE invoice DROP CONSTRAINT fk_invoice_parking_session;
ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE;
ALTER TABLE invoice
    ADD CONSTRAINT fk_invoice_parking_session
        FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;

ALTER TABLE zone_violation_report DROP CONSTRAINT fk_zvr_session;
ALTER TABLE zone_violation_report DROP CONSTRAINT fk_zvr_zone;
ALTER TABLE zone_violation_report DROP CONSTRAINT fk_zvr_slot;
ALTER TABLE zone_violation_report
    ADD CONSTRAINT fk_zvr_session
        FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE zone_violation_report
    ADD CONSTRAINT fk_zvr_zone
        FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE;
ALTER TABLE zone_violation_report
    ADD CONSTRAINT fk_zvr_slot
        FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE CASCADE;

ALTER TABLE pricing_rules DROP CONSTRAINT fk_pricing_rules_parking;
ALTER TABLE pricing_rules
    ADD CONSTRAINT fk_pricing_rules_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE payment_intents DROP CONSTRAINT fk_payment_intents_parking_session;
ALTER TABLE payment_intents DROP CONSTRAINT fk_payment_intents_pricing_rule;
ALTER TABLE payment_intents
    ADD CONSTRAINT fk_payment_intents_parking_session
        FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE payment_intents
    ADD CONSTRAINT fk_payment_intents_pricing_rule
        FOREIGN KEY (pricing_rule_id) REFERENCES pricing_rules(id) ON DELETE SET NULL;

ALTER TABLE fire_extinguishers DROP CONSTRAINT fk_fire_extinguishers_parking;
ALTER TABLE fire_extinguishers DROP CONSTRAINT fk_fire_extinguishers_floor;
ALTER TABLE fire_extinguishers DROP CONSTRAINT fk_fire_extinguishers_zone;
ALTER TABLE fire_extinguishers
    ADD CONSTRAINT fk_fire_extinguishers_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE fire_extinguishers
    ADD CONSTRAINT fk_fire_extinguishers_floor
        FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE;
ALTER TABLE fire_extinguishers
    ADD CONSTRAINT fk_fire_extinguishers_zone
        FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE SET NULL;

ALTER TABLE fire_extinguisher_inspections DROP CONSTRAINT fk_fire_extinguisher_inspections_extinguisher;
ALTER TABLE fire_extinguisher_inspections
    ADD CONSTRAINT fk_fire_extinguisher_inspections_extinguisher
        FOREIGN KEY (fire_extinguisher_id) REFERENCES fire_extinguishers(id) ON DELETE CASCADE;

ALTER TABLE parking_penalty_rules DROP CONSTRAINT fk_penalty_rules_parking;
ALTER TABLE parking_penalty_rules
    ADD CONSTRAINT fk_penalty_rules_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;

ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_parking;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_rule;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_target_session;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_victim_session;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_offender_session;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_reported_slot;
ALTER TABLE parking_penalty_cases DROP CONSTRAINT fk_penalty_cases_reassigned_slot;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_rule
        FOREIGN KEY (rule_id) REFERENCES parking_penalty_rules(id) ON DELETE SET NULL;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_target_session
        FOREIGN KEY (target_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_victim_session
        FOREIGN KEY (victim_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_offender_session
        FOREIGN KEY (offender_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_reported_slot
        FOREIGN KEY (reported_slot_id) REFERENCES slots(id) ON DELETE CASCADE;
ALTER TABLE parking_penalty_cases
    ADD CONSTRAINT fk_penalty_cases_reassigned_slot
        FOREIGN KEY (reassigned_slot_id) REFERENCES slots(id) ON DELETE CASCADE;

ALTER TABLE staff_cash_shifts DROP CONSTRAINT fk_staff_cash_shifts_parking;
ALTER TABLE staff_cash_shifts DROP CONSTRAINT fk_staff_cash_shifts_kiosk;
ALTER TABLE staff_cash_shifts
    ADD CONSTRAINT fk_staff_cash_shifts_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE staff_cash_shifts
    ADD CONSTRAINT fk_staff_cash_shifts_kiosk
        FOREIGN KEY (kiosk_id) REFERENCES kiosk(id) ON DELETE CASCADE;

ALTER TABLE staff_cash_transactions DROP CONSTRAINT fk_staff_cash_transactions_shift;
ALTER TABLE staff_cash_transactions DROP CONSTRAINT fk_staff_cash_transactions_parking;
ALTER TABLE staff_cash_transactions DROP CONSTRAINT fk_staff_cash_transactions_kiosk;
ALTER TABLE staff_cash_transactions DROP CONSTRAINT fk_staff_cash_transactions_session;
ALTER TABLE staff_cash_transactions DROP CONSTRAINT fk_staff_cash_transactions_penalty_case;
ALTER TABLE staff_cash_transactions
    ADD CONSTRAINT fk_staff_cash_transactions_shift
        FOREIGN KEY (shift_id) REFERENCES staff_cash_shifts(id) ON DELETE CASCADE;
ALTER TABLE staff_cash_transactions
    ADD CONSTRAINT fk_staff_cash_transactions_parking
        FOREIGN KEY (parking_id) REFERENCES parkings(id) ON DELETE CASCADE;
ALTER TABLE staff_cash_transactions
    ADD CONSTRAINT fk_staff_cash_transactions_kiosk
        FOREIGN KEY (kiosk_id) REFERENCES kiosk(id) ON DELETE CASCADE;
ALTER TABLE staff_cash_transactions
    ADD CONSTRAINT fk_staff_cash_transactions_session
        FOREIGN KEY (parking_session_id) REFERENCES parking_sessions(id) ON DELETE CASCADE;
ALTER TABLE staff_cash_transactions
    ADD CONSTRAINT fk_staff_cash_transactions_penalty_case
        FOREIGN KEY (penalty_case_id) REFERENCES parking_penalty_cases(id) ON DELETE CASCADE;

-- Legacy records were already logically removed. Purging them means all subsequent facility
-- reads can operate on physical rows only, without reactivating any prior deleted record.
DELETE FROM parkings WHERE is_deleted = TRUE;
DELETE FROM floors WHERE is_deleted = TRUE;
DELETE FROM zones WHERE is_deleted = TRUE;
DELETE FROM slots WHERE is_deleted = TRUE;
