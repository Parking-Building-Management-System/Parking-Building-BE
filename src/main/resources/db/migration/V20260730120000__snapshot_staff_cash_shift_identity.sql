ALTER TABLE staff_cash_shifts
    ADD COLUMN staff_name_snapshot VARCHAR(255),
    ADD COLUMN staff_username_snapshot VARCHAR(255);

UPDATE staff_cash_shifts shift
SET staff_name_snapshot = staff.full_name,
    staff_username_snapshot = staff.username
FROM users staff
WHERE staff.id = shift.staff_id;

ALTER TABLE staff_cash_shifts
    ALTER COLUMN staff_name_snapshot SET NOT NULL,
    ALTER COLUMN staff_username_snapshot SET NOT NULL;
