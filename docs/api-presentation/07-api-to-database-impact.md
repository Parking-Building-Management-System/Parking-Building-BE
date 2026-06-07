# API To Database Impact

This file helps explain that SmartPark APIs are not only CRUD. Many endpoints change operational state, enforce tenant boundaries, and coordinate several tables.

## Auth, Session, and Device Trust

Endpoints:

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `POST /admin/sessions/{sessionId}/revoke`
- `POST /admin/devices/{deviceId}/revoke`
- `POST /manager/device-approvals/{id}/approve`
- `POST /manager/device-approvals/{id}/reject`
- `POST /manager/devices/{id}/revoke`

Reads:

- `users`
- `tenants`
- `roles`
- `user_roles`
- `permissions`
- `role_permissions`
- `devices`
- `sessions`
- `kiosk`
- `kiosk_staff`

Writes:

- `devices` for pending/approved/suspended device state.
- `sessions` for login, refresh JTI rotation, logout, force logout, revoke.
- Redis/session authorization cache and revoke markers.
- `audit_logs` for admin security actions.

State transitions:

- `device PENDING -> APPROVED`
- `device APPROVED -> SUSPENDED`
- `session ACTIVE -> REVOKED`
- `refresh_jti old -> new`

## Tenant and System Admin

Endpoints:

- `GET /admin/tenants`
- `POST /admin/tenants`
- `PATCH /admin/tenants/{id}/status`
- `GET|POST|PUT|DELETE /admin/master-data/vehicle-types`
- `GET /admin/master-data/roles`
- `GET /admin/permissions/tree`
- `GET /admin/roles/{roleId}/permissions`
- `PUT /admin/roles/{roleId}/permissions`
- `POST|PUT|DELETE /admin/permissions`
- `GET /admin/audit/logs`
- `GET /admin/system-health/*`

Reads:

- `tenants`
- `users`
- `roles`
- `user_roles`
- `permissions`
- `role_permissions`
- `vehicle_types`
- `audit_logs`
- `api_traffic_logs`

Writes:

- `tenants` when provisioning or toggling status.
- `users` and `user_roles` for first manager.
- `vehicle_types` for master data changes.
- `permissions` for permission definition changes.
- `role_permissions` for role grant replacement.
- `sessions` when tenant suspension revokes users.
- caches for tenant, vehicle type, and authorization state.

State transitions:

- `tenant ACTIVE <-> SUSPENDED`
- `vehicle_type active/deleted flags changed`
- `permission ACTIVE -> INACTIVE`
- role grant set is fully replaced.

## Facility Setup

Endpoints:

- `POST /manager/parkings`
- `POST /manager/parkings/{parkingId}/floors`
- `POST /manager/floors/{floorId}/zones`
- `POST /manager/zones/{zoneId}/slots`
- `PUT/PATCH/DELETE` manager facility and slot endpoints
- `POST /manager/slots/import`
- `PATCH /manager/floors/{id}/map`
- `PATCH /manager/slots/{id}/coordinate`
- `PATCH /manager/slots/coordinates`

Reads:

- `parkings`
- `floors`
- `zones`
- `slots`
- `vehicle_types`
- tenant context from JWT

Writes:

- `parkings`
- `floors`
- `zones`
- `slots`
- `floors.map_image_url`
- `slots.x_coordinate`
- `slots.y_coordinate`
- soft-delete flags on facility records.

State transitions:

- parking/floor/zone status changes.
- slot `AVAILABLE`, `OCCUPIED`, `MAINTENANCE`, or configured status changes.
- facility records become soft-deleted only when child constraints allow it.

## RFID Cards

Endpoints:

- `GET /manager/rfid-cards`
- `POST /manager/rfid-cards/generate`
- `PATCH /manager/rfid-cards/{id}/status`
- `GET /staff/rfid-cards/available`

Reads:

- `rfid_cards`
- `parking_sessions` to exclude cards already in use.

Writes:

- `rfid_cards` new generated cards with unique `code`, `uid`, and `qr_token`.
- `rfid_cards.status` updates.

State transitions:

- card created as active/configured status.
- card `ACTIVE <-> SUSPENDED/INACTIVE` depending request.
- check-in links card to active session indirectly through `parking_sessions.rfid_card_id`.

## Staff, Kiosk, and Device Setup

Endpoints:

- `POST /manager/staff`
- `PATCH /manager/staff/{id}/status`
- `POST /manager/staff/{id}/reset-password`
- `POST /manager/kiosks`
- `PATCH /manager/kiosks/{id}/status`
- `POST /manager/kiosks/{id}/staff/{staffId}`
- `DELETE /manager/kiosks/{id}/staff/{staffId}`
- `POST /manager/device-approvals/{id}/approve`

Reads:

- `users`
- `roles`
- `user_roles`
- `kiosk`
- `kiosk_staff`
- `devices`
- `parkings`

Writes:

- `users` for staff create/update/status/password hash.
- `user_roles` for STAFF role assignment.
- `kiosk` for gate records.
- `kiosk_staff` for active assignments.
- `devices` for approval/reject/revoke and kiosk binding.

State transitions:

- staff `ACTIVE -> INACTIVE` or back.
- kiosk `ACTIVE -> INACTIVE` or back.
- staff assignment active/inactive.
- device `PENDING -> APPROVED` with `kiosk_id`.

## Parking Session Entry and Exit

Endpoint:

- `POST /staff/parking-sessions/check-in`

Reads:

- `tenants`
- `parkings`
- `rfid_cards`
- `parking_sessions`
- `slots`
- `zones`
- `vehicle_types`
- `kiosk`
- `kiosk_staff`

Writes:

- `parking_sessions`
- `slots.status`

State transition:

- `slot AVAILABLE -> OCCUPIED`
- `parking_session created ACTIVE`
- RFID card becomes linked to active session through `parking_sessions.rfid_card_id`

Endpoint:

- `POST /staff/parking-sessions/exit-preview`

Reads:

- `rfid_cards`
- `parking_sessions`
- `pricing_rules`
- `kiosk`
- `kiosk_staff`

Writes:

- none.

State transition:

- no DB transition. It returns a decision based on current session/payment/grace state.

Endpoint:

- `POST /staff/parking-sessions/complete-exit`

Reads:

- `parking_sessions`
- `rfid_cards`
- `slots`
- `pricing_rules`
- `kiosk`
- `kiosk_staff`

Writes:

- `parking_sessions.status`
- `parking_sessions.check_out_at`
- `parking_sessions.payment_status`
- `parking_sessions.payment_method`
- `parking_sessions.total_amount`
- `slots.status`

State transition:

- `parking_session ACTIVE -> COMPLETED`
- `slot OCCUPIED -> AVAILABLE`
- unpaid cash exit: `payment_status -> CASH_COLLECTED`
- paid grace surcharge: `payment_status -> SURCHARGE_COLLECTED`

## Driver PWA

Endpoints:

- `GET /pwa/cards/{qrToken}/active-session`
- `GET /pwa/cards/{qrToken}/checkout-quote`

Reads:

- `rfid_cards`
- `parking_sessions`
- `parkings`
- `floors`
- `zones`
- `slots`
- `pricing_rules`

Writes:

- none.

State transition:

- none. These public QR-token APIs expose current active session/quote state.

## Pricing and Payment

Endpoints:

- `POST /manager/pricing/rules`
- `PUT /manager/pricing/rules/{id}`
- `PATCH /manager/pricing/rules/{id}/status`
- `DELETE /manager/pricing/rules/{id}`
- `POST /manager/pricing/rules/{id}/preview`

Reads:

- `pricing_rules`
- `parkings`
- `vehicle_types`

Writes:

- `pricing_rules`

State transition:

- pricing rule created/updated.
- pricing rule `ACTIVE -> INACTIVE`.
- preview is read-only.

Endpoint:

- `POST /pwa/cards/{qrToken}/payment-intents`

Reads:

- `rfid_cards`
- `parking_sessions`
- `pricing_rules`
- `payment_intents`

Writes:

- `payment_intents`
- `parking_sessions` for zero-amount immediate payment.

State transition:

- `payment_intent created PENDING`
- zero amount: `payment_intent PENDING -> PAID`, `parking_session.payment_status -> PAID`, `exit_deadline` set.

Endpoint:

- `POST /payments/webhooks/payos`

Reads:

- `payment_intents`
- `parking_sessions`
- `pricing_rules`

Writes:

- `payment_webhook_logs`
- `payment_intents.status`
- `payment_intents.paid_at`
- `parking_sessions.payment_status`
- `parking_sessions.payment_method`
- `parking_sessions.payment_reference`
- `parking_sessions.paid_at`
- `parking_sessions.exit_deadline`
- `parking_sessions.total_amount`

State transition:

- webhook log `RECEIVED -> PROCESSED/FAILED/IGNORED`
- `payment_intent PENDING -> PAID`
- `parking_session payment_status -> PAID`
- exit grace deadline created.

Endpoint:

- `GET /pwa/payment-intents/{orderCode}`

Reads:

- `payment_intents`
- `parking_sessions`
- `rfid_cards`

Writes:

- none.

State transition:

- none. Response may show live `EXPIRED` for old pending intent without mutating DB.

## Fire Safety, Inspection, and Photo

Endpoints:

- `POST /manager/fire-extinguishers`
- `PUT /manager/fire-extinguishers/{id}`
- `PATCH /manager/fire-extinguishers/{id}/status`
- `PATCH /manager/fire-extinguishers/{id}/coordinate`
- `DELETE /manager/fire-extinguishers/{id}`
- `GET /manager/floors/{floorId}/fire-safety-map`
- `GET /manager/fire-inspections/logs`

Reads:

- `fire_extinguishers`
- `parkings`
- `floors`
- `zones`
- `fire_extinguisher_inspections`
- storage service for photo display/download URLs where needed.

Writes:

- `fire_extinguishers`
- `fire_extinguishers.status`
- `fire_extinguishers.x_coordinate`
- `fire_extinguishers.y_coordinate`
- `fire_extinguishers.is_deleted`

State transition:

- extinguisher created.
- map pin updated.
- extinguisher active/maintenance/expired/status fields updated.
- extinguisher soft-deleted.

Endpoint:

- `GET /staff/fire-inspections/due`

Reads:

- `kiosk`
- `kiosk_staff`
- `fire_extinguishers`

Writes:

- none.

Endpoint:

- `POST /staff/fire-inspections/photos/presign-upload`

Reads:

- `kiosk`
- `kiosk_staff`
- storage config.

Writes:

- no DB writes.
- client later writes object bytes to MinIO/S3 using presigned URL.

Endpoint:

- `POST /staff/fire-inspections`

Reads:

- `fire_extinguishers`
- `kiosk`
- `kiosk_staff`
- `users`

Writes:

- `fire_extinguisher_inspections`
- `fire_extinguishers.last_inspected_at`
- `fire_extinguishers.next_inspection_at`
- `fire_extinguishers.status`

State transition:

- inspection log created.
- extinguisher inspection schedule moves forward.
- status changes according to inspection result.
- optional `photo_object_key` links uploaded evidence to the log.
