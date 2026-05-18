-- Seed RBAC and demo identities for the authentication/authorization feature.
-- Default password for all seeded users: Password@123

INSERT INTO permissions (id, name, scope, module, resource, label, action)
VALUES
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.CREATE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.CREATE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.VIEW')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.VIEW',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.UPDATE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.UPDATE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'UPDATE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.TENANT.DELETE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.TENANT.DELETE',
        'Quyền trên app',
        'HE_THONG',
        'Tenant',
        'Khách hàng SaaS',
        'DELETE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.USER.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.USER.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'User',
        'Tài khoản người dùng',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.ROLE.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.ROLE.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Role',
        'Vai trò',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.PERMISSION.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.PERMISSION.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Permission',
        'Quyền hạn',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.CONFIG.MANAGE')::uuid,
        'QUYEN_TREN_APP.HE_THONG.CONFIG.MANAGE',
        'Quyền trên app',
        'HE_THONG',
        'Config',
        'Cấu hình hệ thống',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.HE_THONG.AUDIT_LOG.VIEW')::uuid,
        'QUYEN_TREN_APP.HE_THONG.AUDIT_LOG.VIEW',
        'Quyền trên app',
        'HE_THONG',
        'AuditLog',
        'Nhật ký hệ thống',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.USER.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.USER.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'User',
        'Nhân sự bãi xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.TOA_NHA.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.TOA_NHA.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'Facility',
        'Tòa nhà gửi xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.TOA_NHA.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.TOA_NHA.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'Facility',
        'Tòa nhà gửi xe',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'VehicleType',
        'Loại phương tiện',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.TANG_KHU_VUC.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.TANG_KHU_VUC.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'ParkingZone',
        'Tầng và khu vực đỗ',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.SLOT.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.SLOT.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSlot',
        'Slot đỗ xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.SLOT.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.SLOT.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSlot',
        'Slot đỗ xe',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.SLOT.UPDATE_STATUS')::uuid,
        'QUYEN_TREN_APP.BAI_XE.SLOT.UPDATE_STATUS',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSlot',
        'Trạng thái slot đỗ',
        'UPDATE_STATUS'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.BANG_GIA.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.BANG_GIA.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'PricingPolicy',
        'Bảng giá và chính sách phí',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.BANG_GIA.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.BANG_GIA.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'PricingPolicy',
        'Bảng giá và chính sách phí',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CREATE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CREATE',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSession',
        'Lượt gửi xe',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.LUOT_GUI.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSession',
        'Lượt gửi xe',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CHECK_OUT')::uuid,
        'QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CHECK_OUT',
        'Quyền trên app',
        'BAI_XE',
        'ParkingSession',
        'Xe ra bãi',
        'CHECK_OUT'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.VE_GUI_XE.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.VE_GUI_XE.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'ParkingTicket',
        'Vé gửi xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.COLLECT')::uuid,
        'QUYEN_TREN_APP.BAI_XE.THANH_TOAN.COLLECT',
        'Quyền trên app',
        'BAI_XE',
        'Payment',
        'Thu phí gửi xe',
        'COLLECT'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.PAY')::uuid,
        'QUYEN_TREN_APP.BAI_XE.THANH_TOAN.PAY',
        'Quyền trên app',
        'BAI_XE',
        'Payment',
        'Thanh toán phí gửi xe',
        'PAY'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.THANH_TOAN.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'Payment',
        'Thanh toán và doanh thu',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.DAT_CHO.CREATE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.DAT_CHO.CREATE',
        'Quyền trên app',
        'BAI_XE',
        'Reservation',
        'Đặt chỗ trước',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.DAT_CHO.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.DAT_CHO.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'Reservation',
        'Đặt chỗ trước',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.BAO_CAO.VIEW')::uuid,
        'QUYEN_TREN_APP.BAI_XE.BAO_CAO.VIEW',
        'Quyền trên app',
        'BAI_XE',
        'Report',
        'Báo cáo vận hành và doanh thu',
        'VIEW'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.SU_CO.CREATE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.SU_CO.CREATE',
        'Quyền trên app',
        'BAI_XE',
        'Incident',
        'Sự cố trong bãi xe',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.SU_CO.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.SU_CO.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'Incident',
        'Xử lý sự cố trong bãi xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.PHAN_HOI.CREATE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.PHAN_HOI.CREATE',
        'Quyền trên app',
        'BAI_XE',
        'Feedback',
        'Phản hồi người gửi xe',
        'CREATE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.PHAN_HOI.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.PHAN_HOI.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'Feedback',
        'Xử lý phản hồi người gửi xe',
        'MANAGE'
    ),
    (
        md5('QUYEN_TREN_APP.BAI_XE.AI_PHAN_BO.MANAGE')::uuid,
        'QUYEN_TREN_APP.BAI_XE.AI_PHAN_BO.MANAGE',
        'Quyền trên app',
        'BAI_XE',
        'AiAllocation',
        'AI tối ưu phân bổ chỗ đỗ',
        'MANAGE'
    )
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name, "desc")
VALUES
    (
        md5('SYSTEM_ADMIN')::uuid,
        'SYSTEM_ADMIN',
        'System Admin - chủ phần mềm SaaS, quản trị tenant, tài khoản, phân quyền và cấu hình hệ thống'
    ),
    (
        md5('PARKING_MANAGER')::uuid,
        'PARKING_MANAGER',
        'Parking Manager - chủ tenant, quản lý vận hành tòa nhà gửi xe'
    ),
    (
        md5('STAFF')::uuid,
        'STAFF',
        'Staff - nhân viên/bảo vệ xử lý xe vào ra, thu phí và sự cố tại bãi'
    ),
    (
        md5('PARKING_USER')::uuid,
        'PARKING_USER',
        'Parking User - người gửi xe, xem bãi xe, đặt chỗ, theo dõi lượt gửi và thanh toán'
    )
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5(r.name || ':' || p.name)::uuid, r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

WITH manager_permissions(name) AS (
    VALUES
        ('QUYEN_TREN_APP.BAI_XE.USER.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.TOA_NHA.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.TOA_NHA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.TANG_KHU_VUC.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.UPDATE_STATUS'),
        ('QUYEN_TREN_APP.BAI_XE.BANG_GIA.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.BANG_GIA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.VE_GUI_XE.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.DAT_CHO.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.BAO_CAO.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.SU_CO.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.PHAN_HOI.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.AI_PHAN_BO.MANAGE')
)
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5(r.name || ':' || p.name)::uuid, r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
JOIN manager_permissions mp ON mp.name = p.name
WHERE r.name = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

WITH staff_permissions(name) AS (
    VALUES
        ('QUYEN_TREN_APP.BAI_XE.TOA_NHA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.UPDATE_STATUS'),
        ('QUYEN_TREN_APP.BAI_XE.BANG_GIA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CREATE'),
        ('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.CHECK_OUT'),
        ('QUYEN_TREN_APP.BAI_XE.VE_GUI_XE.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.COLLECT'),
        ('QUYEN_TREN_APP.BAI_XE.DAT_CHO.MANAGE'),
        ('QUYEN_TREN_APP.BAI_XE.SU_CO.CREATE'),
        ('QUYEN_TREN_APP.BAI_XE.SU_CO.MANAGE')
)
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5(r.name || ':' || p.name)::uuid, r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
JOIN staff_permissions sp ON sp.name = p.name
WHERE r.name = 'STAFF'
ON CONFLICT (role_id, permission_id) DO NOTHING;

WITH parking_user_permissions(name) AS (
    VALUES
        ('QUYEN_TREN_APP.BAI_XE.TOA_NHA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.SLOT.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.BANG_GIA.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.LUOT_GUI.VIEW'),
        ('QUYEN_TREN_APP.BAI_XE.THANH_TOAN.PAY'),
        ('QUYEN_TREN_APP.BAI_XE.DAT_CHO.CREATE'),
        ('QUYEN_TREN_APP.BAI_XE.SU_CO.CREATE'),
        ('QUYEN_TREN_APP.BAI_XE.PHAN_HOI.CREATE')
)
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT md5(r.name || ':' || p.name)::uuid, r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
JOIN parking_user_permissions pup ON pup.name = p.name
WHERE r.name = 'PARKING_USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO tenants (id, name, slug, email_contact, status, is_deleted)
VALUES
    (
        md5('smartpark-saas')::uuid,
        'SmartPark SaaS',
        'smartpark-saas',
        'admin@smartpark.local',
        'ACTIVE',
        false
    ),
    (
        md5('demo-parking-tower')::uuid,
        'Demo Parking Tower',
        'demo-parking-tower',
        'manager@demo-parking.local',
        'ACTIVE',
        false
    )
ON CONFLICT (slug) DO NOTHING;

INSERT INTO users (id, tenant_id, username, password, full_name, phone, status, created_by, is_deleted)
VALUES
    (
        md5('system.admin@smartpark.local')::uuid,
        md5('smartpark-saas')::uuid,
        'system.admin@smartpark.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'System Administrator',
        '0900000001',
        'ACTIVE',
        NULL,
        false
    ),
    (
        md5('manager@demo-parking.local')::uuid,
        md5('demo-parking-tower')::uuid,
        'manager@demo-parking.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Parking Manager',
        '0900000002',
        'ACTIVE',
        md5('system.admin@smartpark.local')::uuid,
        false
    ),
    (
        md5('staff@demo-parking.local')::uuid,
        md5('demo-parking-tower')::uuid,
        'staff@demo-parking.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Parking Staff',
        '0900000003',
        'ACTIVE',
        md5('manager@demo-parking.local')::uuid,
        false
    ),
    (
        md5('driver@demo-parking.local')::uuid,
        md5('demo-parking-tower')::uuid,
        'driver@demo-parking.local',
        '$2a$10$q4lJo1nNnql5H3n5g5b14unZ2/B4I.EeIOfJM/E/raMU7okw1E5Fm',
        'Parking User',
        '0900000004',
        'ACTIVE',
        md5('manager@demo-parking.local')::uuid,
        false
    )
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (id, user_id, role_id)
VALUES
    (
        md5('system.admin@smartpark.local:SYSTEM_ADMIN')::uuid,
        md5('system.admin@smartpark.local')::uuid,
        md5('SYSTEM_ADMIN')::uuid
    ),
    (
        md5('manager@demo-parking.local:PARKING_MANAGER')::uuid,
        md5('manager@demo-parking.local')::uuid,
        md5('PARKING_MANAGER')::uuid
    ),
    (
        md5('staff@demo-parking.local:STAFF')::uuid,
        md5('staff@demo-parking.local')::uuid,
        md5('STAFF')::uuid
    ),
    (
        md5('driver@demo-parking.local:PARKING_USER')::uuid,
        md5('driver@demo-parking.local')::uuid,
        md5('PARKING_USER')::uuid
    )
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO devices (id, user_id, fingerprint, label, status, approved_by, approved_at, expires_at)
VALUES
    (
        md5('system.admin@smartpark.local:seed-system-admin-device')::uuid,
        md5('system.admin@smartpark.local')::uuid,
        'seed-system-admin-device',
        'Seed System Admin Device',
        'APPROVED',
        md5('system.admin@smartpark.local')::uuid,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        md5('manager@demo-parking.local:seed-manager-device')::uuid,
        md5('manager@demo-parking.local')::uuid,
        'seed-manager-device',
        'Seed Parking Manager Device',
        'APPROVED',
        md5('system.admin@smartpark.local')::uuid,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        md5('staff@demo-parking.local:seed-staff-device')::uuid,
        md5('staff@demo-parking.local')::uuid,
        'seed-staff-device',
        'Seed Staff Device',
        'APPROVED',
        md5('manager@demo-parking.local')::uuid,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        md5('driver@demo-parking.local:seed-driver-device')::uuid,
        md5('driver@demo-parking.local')::uuid,
        'seed-driver-device',
        'Seed Parking User Device',
        'APPROVED',
        md5('manager@demo-parking.local')::uuid,
        CURRENT_TIMESTAMP,
        NULL
    )
ON CONFLICT (user_id, fingerprint) DO NOTHING;
