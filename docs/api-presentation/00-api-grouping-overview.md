# API Grouping Overview

SmartPark APIs are divided into exactly three presentation groups so each backend member can explain one coherent business area.

## Group 1 - Foundation, Identity & Administration APIs

Purpose: system foundation, authentication, tenant/account control, security, health, and admin monitoring.

Speaker focus: "Who can access the system, how tenants/users/permissions are managed, and how administrators monitor the platform."

Actors: `SYSTEM_ADMIN`, `PARKING_MANAGER`, `STAFF`.

Tags/controllers:

- `AuthenticationController` - `Authentication`
- `AdminTenantManagementController` - `Admin Tenants`
- `AdminMasterDataController` - `Admin Master Data`
- `AdminPermissionController` - `System Admin Permissions`
- `AdminSystemHealthController` - `System Admin Health`
- `AdminAuditSecurityController` - `System Admin Audit & Security`
- Legacy tenant controller exists as `TenantController`, but the presentation should prioritize `/admin/tenants`.

Endpoint paths:

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `POST /auth/admin/users/{userId}/force-logout`
- `GET /admin/tenants`
- `POST /admin/tenants`
- `PATCH /admin/tenants/{id}/status`
- `GET /admin/master-data/vehicle-types`
- `POST /admin/master-data/vehicle-types`
- `PUT /admin/master-data/vehicle-types/{id}`
- `DELETE /admin/master-data/vehicle-types/{id}`
- `GET /admin/master-data/roles`
- `GET /admin/permissions/tree`
- `GET /admin/roles`
- `GET /admin/roles/{roleId}/permissions`
- `PUT /admin/roles/{roleId}/permissions`
- `POST /admin/permissions`
- `PUT /admin/permissions/{id}`
- `DELETE /admin/permissions/{id}`
- `GET /admin/system-health/summary`
- `GET /admin/system-health/services`
- `GET /admin/system-health/traffic`
- `GET /admin/system-health/top-endpoints`
- `GET /admin/system-health/errors`
- `GET /admin/audit/logs`
- `GET /admin/sessions`
- `POST /admin/users/{userId}/force-logout`
- `POST /admin/sessions/{sessionId}/revoke`
- `GET /admin/devices`
- `POST /admin/devices/{deviceId}/revoke`

Why these APIs belong together:

- They establish the platform boundary: authenticated users, tenant workspaces, global master data, permissions, sessions, and device trust.
- They are mostly cross-tenant or global admin APIs.
- They explain security controls before the operational demo begins.

Demo story:

1. System Admin logs in.
2. Admin provisions a tenant and first manager.
3. Admin reviews vehicle types, roles, and permission tree.
4. Admin checks health/traffic.
5. Admin reviews audit logs and revokes a risky session/device.

Database areas affected:

- `tenants`, `users`, `roles`, `permissions`, `user_roles`, `role_permissions`
- `devices`, `sessions`
- `vehicle_types`
- `audit_logs`, `api_traffic_logs`

## Group 2 - Parking Facility & Daily Operations APIs

Purpose: manager setup and staff daily parking operations.

Speaker focus: "How a parking lot is configured and how vehicles enter/exit through staff kiosks and PWA guidance."

Actors: `PARKING_MANAGER`, `STAFF`, Driver/PWA.

Tags/controllers:

- `ManagerFacilityController` - `Manager Facility`
- `ManagerSlotManagementController` and `ManagerSlotController` - `Manager Slots`
- `ManagerFacilityMapController` - `Manager Facility Map`
- `ManagerStorageController` - `Manager Storage`
- `ManagerRfidCardController` - `Manager RFID Cards`
- `ManagerStaffController` - `Manager Staff Accounts`
- `ManagerKioskController` - `Manager Kiosks`, `Manager Kiosk Staff`
- `ManagerDeviceApprovalController` - `Manager Staff Devices`
- `StaffRfidCardController` - `Staff RFID Cards`
- `StaffParkingSessionController` - `Staff Parking Sessions`
- `PwaCardSessionController` - `PWA Driver`

Endpoint paths:

- `GET|POST /manager/parkings`
- `GET|PUT /manager/parkings/{id}`
- `PATCH /manager/parkings/{id}/status`
- `GET /manager/parkings/{id}/topology`
- `GET|POST /manager/parkings/{parkingId}/floors`
- `GET|PUT|DELETE /manager/floors/{id}`
- `GET|POST /manager/floors/{floorId}/zones`
- `GET|PUT|DELETE /manager/zones/{id}`
- `POST /manager/zones/{zoneId}/slots`
- `GET|PUT|DELETE /manager/slots/{id}`
- `GET /manager/slots`
- `PATCH /manager/slots/{id}/status`
- `PATCH /manager/slots/bulk-status`
- `POST /manager/slots/import`
- `GET /manager/slots/export`
- `PATCH /manager/floors/{id}/map`
- `GET /manager/floors/{id}/map`
- `PATCH /manager/slots/{id}/coordinate`
- `PATCH /manager/slots/coordinates`
- `POST /manager/storage/presign-upload`
- `GET /manager/storage/presign-download`
- `GET /manager/rfid-cards`
- `POST /manager/rfid-cards/generate`
- `PATCH /manager/rfid-cards/{id}/status`
- `GET|POST /manager/staff`
- `GET|PUT /manager/staff/{id}`
- `PATCH /manager/staff/{id}/status`
- `POST /manager/staff/{id}/reset-password`
- `GET|POST /manager/kiosks`
- `GET|PUT|DELETE /manager/kiosks/{id}`
- `PATCH /manager/kiosks/{id}/status`
- `GET /manager/kiosks/{id}/staff`
- `POST /manager/kiosks/{id}/staff/{staffId}`
- `DELETE /manager/kiosks/{id}/staff/{staffId}`
- `GET /manager/device-approvals`
- `POST /manager/device-approvals/{id}/approve`
- `POST /manager/device-approvals/{id}/reject`
- `POST /manager/devices/{id}/revoke`
- `GET /staff/rfid-cards/available`
- `POST /staff/parking-sessions/check-in`
- `POST /staff/parking-sessions/exit-preview`
- `POST /staff/parking-sessions/complete-exit`
- `GET /pwa/cards/{qrToken}/active-session`
- `GET /pwa/cards/{qrToken}/checkout-quote`

Why these APIs belong together:

- They represent parking configuration and daily gate operations.
- Manager setup produces the objects staff APIs consume: parking, floors, zones, slots, maps, cards, staff, kiosks, and trusted devices.
- Staff entry creates the active session and Driver PWA reads it.
- Staff exit completes the operational lifecycle.

Demo story:

1. Manager checks facility topology.
2. Manager checks map and slot pins.
3. Manager ensures RFID pool and staff/kiosk/device approval exist.
4. Staff at entry kiosk checks in a car.
5. Driver opens QR/PWA guide to the assigned slot.
6. Staff at exit kiosk previews and completes exit.

Database areas affected:

- `parkings`, `floors`, `zones`, `slots`, `rfid_cards`
- `users`, `user_roles`, `kiosk`, `kiosk_staff`, `devices`, `sessions`
- `parking_sessions`
- Storage object keys/URLs for maps are persisted on `floors`.

## Group 3 - Pricing, Payment & Safety Compliance APIs

Purpose: money flow and safety compliance modules.

Speaker focus: "How the system calculates fees, receives online payment, handles exit grace period, and supports fire safety inspection compliance."

Actors: `PARKING_MANAGER`, `STAFF`, Driver/PWA, PayOS.

Tags/controllers:

- `ManagerPricingRuleController` - `Manager Pricing`
- `PwaCardSessionController` - checkout quote part of `PWA Driver`
- `PwaPaymentController` - `PWA Payments`
- `PayosWebhookController` - `Payment Webhooks`
- `ManagerFireExtinguisherController` - `Manager Fire Safety`
- `StaffFireInspectionController` - `Staff Fire Inspections`

Endpoint paths:

- `GET /manager/pricing/rules`
- `POST /manager/pricing/rules`
- `GET /manager/pricing/rules/{id}`
- `PUT /manager/pricing/rules/{id}`
- `PATCH /manager/pricing/rules/{id}/status`
- `DELETE /manager/pricing/rules/{id}`
- `POST /manager/pricing/rules/{id}/preview`
- `GET /pwa/cards/{qrToken}/checkout-quote`
- `POST /pwa/cards/{qrToken}/payment-intents`
- `GET /pwa/payment-intents/{orderCode}`
- `POST /payments/webhooks/payos`
- `GET /manager/fire-extinguishers`
- `POST /manager/fire-extinguishers`
- `GET /manager/fire-extinguishers/{id}`
- `PUT /manager/fire-extinguishers/{id}`
- `PATCH /manager/fire-extinguishers/{id}/status`
- `PATCH /manager/fire-extinguishers/{id}/coordinate`
- `DELETE /manager/fire-extinguishers/{id}`
- `GET /manager/fire-extinguishers/summary`
- `GET /manager/floors/{floorId}/fire-safety-map`
- `GET /manager/fire-inspections/logs`
- `GET /staff/fire-inspections/due`
- `POST /staff/fire-inspections`
- `POST /staff/fire-inspections/photos/presign-upload`

Why these APIs belong together:

- Pricing and payment convert a parking session into a paid exit decision.
- Fire Safety is compliance-oriented and has a parallel lifecycle: setup, map display, staff inspection, manager review.
- Both areas demonstrate that APIs enforce business rules and update important state, not just CRUD records.

Demo story:

1. Manager creates/preview pricing rule.
2. Driver checks quote, creates PayOS payment intent, and polls status.
3. PayOS webhook marks the session paid and sets exit deadline.
4. Manager views fire safety map.
5. Staff completes a due fire inspection with a photo object key.
6. Manager reviews inspection logs.

Database areas affected:

- `pricing_rules`
- `payment_intents`, `payment_webhook_logs`, `parking_sessions`
- `fire_extinguishers`, `fire_extinguisher_inspections`
- Storage object keys for inspection photos.
