# 06. Questions For Owner

| Priority | Question | Why It Matters | Blocks Which Task |
|---|---|---|---|
| P0 | Staff username nội bộ format gì: `tenantCode.staffCode`, mã nhân viên, số điện thoại, hay email nội bộ? | `users.username` đang globally unique; format ảnh hưởng validation, seed, login UX. | Task 7 |
| P0 | Manager bắt buộc có email không, hay có thể dùng username nội bộ? | Admin tenant provisioning hiện dùng `managerEmail` làm username. | Task 4, Task 7 |
| P0 | User/PWA có cần email không hay phone-only? | Quyết định reuse `users` hay tạo `DriverAccount`. | PHASE_4_PWA |
| P0 | VehicleType là global master data hay tenant-specific? | Code hiện là global `vehicle_types`; đổi sang tenant-specific sẽ ảnh hưởng pricing/facility. | Task 5, Task 6, Pricing |
| P0 | Parking do System Admin tạo lúc provision tenant hay Manager tự tạo? | Hiện manager chỉ list/toggle parking; create/update/delete parking MISSING. | Task 4, Task 6 |
| P0 | Mỗi tenant có nhiều parking building không? | Current schema supports many `parkings` per tenant; FE flow cần xác nhận. | Task 6 |
| P0 | Slot code convention là gì? Ví dụ `B1-A-001`, `A-01`, hay tùy tenant? | Import/search/validation phụ thuộc convention. | Task 6 |
| P0 | Staff có được login nhiều kiosk/device đồng thời không? | Ảnh hưởng session policy and device approval/revoke logic. | Task 8 |
| P0 | Staff có bị giới hạn theo kiosk/gate không? | Current `kiosk_staff` exists, nhưng login chưa enforce kiosk assignment. | Task 8, PHASE_2_OPERATION |
| P0 | Device approval temporary 8h lưu thế nào: dùng `devices.expires_at` hay cần `device_approval_requests` riêng? | Entity `Device` có `expiresAt`, nhưng không có request history. | Task 8 |
| P1 | Force open có cần ảnh bắt buộc ở phase đầu không? | Red flag action schema/API cần field evidence required/optional. | PHASE_2_OPERATION |
| P1 | Blind drop có làm phase đầu không? | Nếu có, cần StaffShift/ShiftCashDrop trước billing đầy đủ. | PHASE_2_OPERATION |
| P1 | Subscription billing có cần ngay không? | Subscription entity có nhưng không có API/job; scope lớn. | PHASE_3_BILLING |
| P1 | PWA lazy auth có cần ngay không? | Phone+plate OTP/card QR ảnh hưởng identity design. | PHASE_4_PWA |
| P1 | Payment/VietQR làm mock trước hay tích hợp thật? | Payment entity, webhook, exit pass phụ thuộc lựa chọn. | PHASE_3_BILLING, PHASE_4_PWA |
| P1 | Có cần parking card ngay trong CRUD phase không? | Current `rfid_cards` may not equal physical parking card flow. | PHASE_2_OPERATION |
| P2 | Có cần import Excel slot ngay không? | Code đã có import/export; frontend có cần ngay hay chỉ CRUD row-level? | Task 6 |
| P2 | Có cần upload floor plan image ngay không? | Current Floor/Parking entities không có floor plan image field. | Task 6, PHASE_LATER |
| P2 | Role/permission có cần UI chỉnh sửa không, hay chỉ list role cố định? | Code có role list only; permission CRUD can be dangerous. | Task 4/5 adjacent |
| P2 | Incident taxonomy chuẩn gồm những loại nào? | Need enum/validation for Incident/RedFlagAction. | PHASE_2_OPERATION |
| P2 | Biển số trùng giữa tenant có được phép không? | Spec says Vin/FPT isolation; current `user_vehicle_link` unique `(tenant_id, license_plate)` supports this. | PHASE_2_OPERATION, PHASE_4_PWA |
