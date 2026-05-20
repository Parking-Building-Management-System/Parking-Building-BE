INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES
    (
        md5('tenant-vincom')::uuid,
        'Vincom Parking',
        'vincom',
        'ops@vincom.smartpark.local',
        'ACTIVE',
        false
    ),
    (
        md5('tenant-fpt')::uuid,
        'FPT Campus Parking',
        'fpt',
        'ops@fpt.smartpark.local',
        'ACTIVE',
        false
    )
ON CONFLICT (slug) DO NOTHING;

INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    (
        md5('user-vincom-manager')::uuid,
        md5('tenant-vincom')::uuid,
        'manager@vincom.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Vincom Manager',
        '0911000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('user-vincom-staff')::uuid,
        md5('tenant-vincom')::uuid,
        'staff@vincom.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Vincom Staff',
        '0911000002',
        'ACTIVE',
        md5('user-vincom-manager')::uuid,
        false
    ),
    (
        md5('user-fpt-manager')::uuid,
        md5('tenant-fpt')::uuid,
        'manager@fpt.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'FPT Manager',
        '0922000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('user-fpt-staff')::uuid,
        md5('tenant-fpt')::uuid,
        'staff@fpt.smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'FPT Staff',
        '0922000002',
        'ACTIVE',
        md5('user-fpt-manager')::uuid,
        false
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO vehicle_types (id, name, code, is_active, is_deleted)
VALUES
    (md5('vehicle-type-car')::uuid, 'Car', 'CAR', true, false),
    (md5('vehicle-type-motorbike')::uuid, 'Motorbike', 'MOTORBIKE', true, false)
ON CONFLICT (code) DO NOTHING;

INSERT INTO parkings (id, tenant_id, code, name, address, total_capacity, status, is_deleted)
VALUES
    (
        md5('parking-vincom-dongkhoi')::uuid,
        md5('tenant-vincom')::uuid,
        'VINCOM-DK',
        'Vincom Dong Khoi',
        '72 Le Thanh Ton, District 1, Ho Chi Minh City',
        120,
        'ACTIVE',
        false
    ),
    (
        md5('parking-fpt-campus')::uuid,
        md5('tenant-fpt')::uuid,
        'FPT-HOLA',
        'FPT Hoa Lac Campus',
        'Hoa Lac Hi-Tech Park, Hanoi',
        200,
        'ACTIVE',
        false
    )
ON CONFLICT DO NOTHING;

INSERT INTO zones (id, tenant_id, parking_id, code, name, floor_name, capacity, status, is_deleted)
VALUES
    (
        md5('zone-vincom-b1')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        'B1',
        'Basement 1',
        'B1',
        60,
        'ACTIVE',
        false
    ),
    (
        md5('zone-fpt-a')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        'A',
        'Building A Parking',
        'G',
        100,
        'ACTIVE',
        false
    )
ON CONFLICT DO NOTHING;

INSERT INTO slots (id, tenant_id, parking_id, zone_id, code, slot_number, status, is_deleted)
VALUES
    (
        md5('slot-vincom-b1-a01')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        md5('zone-vincom-b1')::uuid,
        'A01',
        'A01',
        'OCCUPIED',
        false
    ),
    (
        md5('slot-vincom-b1-a02')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        md5('zone-vincom-b1')::uuid,
        'A02',
        'A02',
        'AVAILABLE',
        false
    ),
    (
        md5('slot-vincom-b1-vio')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        md5('zone-vincom-b1')::uuid,
        'A03',
        'A03',
        'OCCUPIED',
        false
    ),
    (
        md5('slot-fpt-a-01')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        md5('zone-fpt-a')::uuid,
        'A-01',
        'A-01',
        'AVAILABLE',
        false
    ),
    (
        md5('slot-fpt-a-02')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        md5('zone-fpt-a')::uuid,
        'A-02',
        'A-02',
        'OCCUPIED',
        false
    )
ON CONFLICT DO NOTHING;

INSERT INTO rfid_cards (id, tenant_id, code, uid, assigned_user_id, status, activated_at, expired_at)
VALUES
    (
        md5('rfid-vincom-001')::uuid,
        md5('tenant-vincom')::uuid,
        'VIN-RFID-001',
        'VIN-UID-001',
        md5('user-vincom-manager')::uuid,
        'ACTIVE',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP + INTERVAL '335 days'
    ),
    (
        md5('rfid-fpt-001')::uuid,
        md5('tenant-fpt')::uuid,
        'FPT-RFID-001',
        'FPT-UID-001',
        md5('user-fpt-manager')::uuid,
        'ACTIVE',
        CURRENT_TIMESTAMP - INTERVAL '45 days',
        CURRENT_TIMESTAMP + INTERVAL '320 days'
    )
ON CONFLICT DO NOTHING;

INSERT INTO user_vehicle_link (
    id, tenant_id, user_id, vehicle_type_id, license_plate, vehicle_label,
    is_default, is_active, is_deleted
)
VALUES
    (
        md5('uvl-vincom-car')::uuid,
        md5('tenant-vincom')::uuid,
        md5('user-vincom-manager')::uuid,
        md5('vehicle-type-car')::uuid,
        '51A-12345',
        'Manager Sedan',
        true,
        true,
        false
    ),
    (
        md5('uvl-fpt-bike')::uuid,
        md5('tenant-fpt')::uuid,
        md5('user-fpt-manager')::uuid,
        md5('vehicle-type-motorbike')::uuid,
        '29B1-54321',
        'Campus Bike',
        true,
        true,
        false
    )
ON CONFLICT DO NOTHING;

INSERT INTO shift (id, tenant_id, parking_id, name, start_time, end_time, is_active)
VALUES
    (
        md5('shift-vincom-morning')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        'Morning',
        TIME '08:00',
        TIME '16:00',
        true
    ),
    (
        md5('shift-vincom-night')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        'Night',
        TIME '16:00',
        TIME '23:59',
        true
    ),
    (
        md5('shift-fpt-morning')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        'Morning',
        TIME '07:00',
        TIME '15:00',
        true
    )
ON CONFLICT DO NOTHING;

INSERT INTO kiosk (id, tenant_id, parking_id, code, name, status, last_heartbeat_at)
VALUES
    (
        md5('kiosk-vincom-main')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        'VIN-KIOSK-01',
        'Vincom Main Gate',
        'ACTIVE',
        CURRENT_TIMESTAMP - INTERVAL '1 minute'
    ),
    (
        md5('kiosk-fpt-main')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        'FPT-KIOSK-01',
        'FPT Main Gate',
        'ACTIVE',
        CURRENT_TIMESTAMP - INTERVAL '2 minutes'
    )
ON CONFLICT DO NOTHING;

INSERT INTO kiosk_staff (id, tenant_id, kiosk_id, staff_user_id, shift_id, assigned_at, is_active)
VALUES
    (
        md5('kiosk-staff-vincom-morning')::uuid,
        md5('tenant-vincom')::uuid,
        md5('kiosk-vincom-main')::uuid,
        md5('user-vincom-staff')::uuid,
        md5('shift-vincom-morning')::uuid,
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        true
    ),
    (
        md5('kiosk-staff-fpt-morning')::uuid,
        md5('tenant-fpt')::uuid,
        md5('kiosk-fpt-main')::uuid,
        md5('user-fpt-staff')::uuid,
        md5('shift-fpt-morning')::uuid,
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        true
    )
ON CONFLICT DO NOTHING;

INSERT INTO parking_sessions (
    id, tenant_id, parking_id, zone_id, slot_id, rfid_card_id, vehicle_type_id,
    user_vehicle_link_id, license_plate, check_in_at, check_out_at, status,
    entry_image_url, exit_image_url, total_amount
)
VALUES
    (
        md5('ps-vincom-active')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        md5('zone-vincom-b1')::uuid,
        md5('slot-vincom-b1-a01')::uuid,
        md5('rfid-vincom-001')::uuid,
        md5('vehicle-type-car')::uuid,
        md5('uvl-vincom-car')::uuid,
        '51A-12345',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        NULL,
        'ACTIVE',
        NULL,
        NULL,
        NULL
    ),
    (
        md5('ps-fpt-completed')::uuid,
        md5('tenant-fpt')::uuid,
        md5('parking-fpt-campus')::uuid,
        md5('zone-fpt-a')::uuid,
        md5('slot-fpt-a-02')::uuid,
        md5('rfid-fpt-001')::uuid,
        md5('vehicle-type-motorbike')::uuid,
        md5('uvl-fpt-bike')::uuid,
        '29B1-54321',
        CURRENT_TIMESTAMP - INTERVAL '4 hours',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        'COMPLETED',
        NULL,
        NULL,
        15000.00
    ),
    (
        md5('ps-vincom-violation')::uuid,
        md5('tenant-vincom')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        md5('zone-vincom-b1')::uuid,
        md5('slot-vincom-b1-vio')::uuid,
        md5('rfid-vincom-001')::uuid,
        md5('vehicle-type-car')::uuid,
        md5('uvl-vincom-car')::uuid,
        '51A-12345',
        CURRENT_TIMESTAMP - INTERVAL '5 hours',
        NULL,
        'VIOLATION',
        NULL,
        NULL,
        NULL
    )
ON CONFLICT DO NOTHING;

INSERT INTO subscriptions (
    id, tenant_id, user_id, user_vehicle_link_id, parking_id, period_string,
    start_date, end_date, monthly_price, status, auto_renew
)
VALUES
    (
        md5('sub-vincom-active-202605')::uuid,
        md5('tenant-vincom')::uuid,
        md5('user-vincom-manager')::uuid,
        md5('uvl-vincom-car')::uuid,
        md5('parking-vincom-dongkhoi')::uuid,
        '2026-05',
        DATE '2026-05-01',
        DATE '2026-05-31',
        2500000.00,
        'ACTIVE',
        true
    ),
    (
        md5('sub-fpt-expired-202604')::uuid,
        md5('tenant-fpt')::uuid,
        md5('user-fpt-manager')::uuid,
        md5('uvl-fpt-bike')::uuid,
        md5('parking-fpt-campus')::uuid,
        '2026-04',
        DATE '2026-04-01',
        DATE '2026-04-30',
        350000.00,
        'EXPIRED',
        false
    )
ON CONFLICT DO NOTHING;

INSERT INTO invoice (
    id, tenant_id, invoice_no, user_id, subscription_id, parking_session_id,
    invoice_type, status, amount, tax_amount, total_amount, issued_at, due_at, paid_at
)
VALUES
    (
        md5('invoice-vincom-sub-202605')::uuid,
        md5('tenant-vincom')::uuid,
        'VIN-INV-202605-001',
        md5('user-vincom-manager')::uuid,
        md5('sub-vincom-active-202605')::uuid,
        NULL,
        'SUBSCRIPTION',
        'PAID',
        2500000.00,
        0.00,
        2500000.00,
        CURRENT_TIMESTAMP - INTERVAL '20 days',
        CURRENT_TIMESTAMP - INTERVAL '10 days',
        CURRENT_TIMESTAMP - INTERVAL '19 days'
    ),
    (
        md5('invoice-fpt-session-001')::uuid,
        md5('tenant-fpt')::uuid,
        'FPT-INV-202605-001',
        md5('user-fpt-manager')::uuid,
        NULL,
        md5('ps-fpt-completed')::uuid,
        'PARKING_SESSION',
        'ISSUED',
        15000.00,
        0.00,
        15000.00,
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        CURRENT_TIMESTAMP + INTERVAL '7 days',
        NULL
    )
ON CONFLICT DO NOTHING;

INSERT INTO zone_violation_report (
    id, tenant_id, parking_session_id, zone_id, slot_id, reported_by, violation_type,
    description, status, occurred_at, resolved_at
)
VALUES
    (
        md5('zvr-vincom-wrong-zone')::uuid,
        md5('tenant-vincom')::uuid,
        md5('ps-vincom-violation')::uuid,
        md5('zone-vincom-b1')::uuid,
        md5('slot-vincom-b1-vio')::uuid,
        md5('user-vincom-staff')::uuid,
        'WRONG_ZONE',
        'Vehicle parked outside the assigned subscription zone.',
        'OPEN',
        CURRENT_TIMESTAMP - INTERVAL '4 hours',
        NULL
    )
ON CONFLICT DO NOTHING;

INSERT INTO webhook_log (
    id, tenant_id, provider, event_type, external_id, payload, status,
    received_at, processed_at, error_message
)
VALUES
    (
        md5('webhook-vincom-payment-success')::uuid,
        md5('tenant-vincom')::uuid,
        'MOMO',
        'payment.success',
        'momo-vincom-001',
        '{"invoiceNo":"VIN-INV-202605-001","amount":2500000}',
        'PROCESSED',
        CURRENT_TIMESTAMP - INTERVAL '19 days',
        CURRENT_TIMESTAMP - INTERVAL '19 days' + INTERVAL '1 minute',
        NULL
    ),
    (
        md5('webhook-fpt-payment-pending')::uuid,
        md5('tenant-fpt')::uuid,
        'VNPAY',
        'payment.created',
        'vnpay-fpt-001',
        '{"invoiceNo":"FPT-INV-202605-001","amount":15000}',
        'RECEIVED',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes',
        NULL,
        NULL
    )
ON CONFLICT DO NOTHING;

INSERT INTO audit_logs (
    id, tenant_id, actor_user_id, action, resource_type, resource_id,
    ip_address, user_agent, metadata, occurred_at
)
VALUES
    (
        md5('audit-vincom-create-sub')::uuid,
        md5('tenant-vincom')::uuid,
        md5('user-vincom-manager')::uuid,
        'CREATE',
        'Subscription',
        md5('sub-vincom-active-202605')::uuid,
        '10.10.1.10',
        'SmartPark Seed',
        '{"source":"seed"}',
        CURRENT_TIMESTAMP - INTERVAL '20 days'
    ),
    (
        md5('audit-fpt-complete-session')::uuid,
        md5('tenant-fpt')::uuid,
        md5('user-fpt-staff')::uuid,
        'COMPLETE',
        'ParkingSession',
        md5('ps-fpt-completed')::uuid,
        '10.20.1.10',
        'SmartPark Seed',
        '{"source":"seed"}',
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    )
ON CONFLICT DO NOTHING;

INSERT INTO notification (
    id, tenant_id, recipient_user_id, title, content, notification_type, status, read_at
)
VALUES
    (
        md5('notification-vincom-violation')::uuid,
        md5('tenant-vincom')::uuid,
        md5('user-vincom-manager')::uuid,
        'Zone violation detected',
        'Vehicle 51A-12345 has a zone violation report.',
        'PARKING',
        'UNREAD',
        NULL
    ),
    (
        md5('notification-fpt-session-invoice')::uuid,
        md5('tenant-fpt')::uuid,
        md5('user-fpt-manager')::uuid,
        'Parking invoice issued',
        'Invoice FPT-INV-202605-001 is waiting for payment.',
        'BILLING',
        'UNREAD',
        NULL
    )
ON CONFLICT DO NOTHING;

INSERT INTO api_traffic_logs (id, method, path, status_code, duration_ms, occurred_at)
VALUES
    (
        md5('api-traffic-login-vincom')::uuid,
        'POST',
        '/auth/login',
        200,
        42,
        CURRENT_TIMESTAMP - INTERVAL '10 minutes'
    ),
    (
        md5('api-traffic-dashboard')::uuid,
        'GET',
        '/dashboard/counters',
        200,
        25,
        CURRENT_TIMESTAMP - INTERVAL '5 minutes'
    )
ON CONFLICT DO NOTHING;
