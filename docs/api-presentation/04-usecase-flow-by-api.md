# Use Case Flow By API

This file is outline-ready for backend presenters. It summarizes what happens from request to database effect.

## Group 1 - Foundation, Identity & Administration

### LoginUseCase - `POST /auth/login`

Actor: public credentials; final role can be `SYSTEM_ADMIN`, `PARKING_MANAGER`, or `STAFF`.

Controller/service: `AuthenticationController.login` -> `AuthenticationServiceImpl.authenticate`.

```text
1. Receive username, password, deviceFingerprint, and optional deviceLabel.
2. Find user by username.
3. If user is missing or password does not match -> INVALID_INFO.
4. Require user status ACTIVE.
5. Load role names.
6. If not SYSTEM_ADMIN, require tenant status ACTIVE.
7. Resolve login device:
   - SYSTEM_ADMIN: auto-create approved device if first seen.
   - PARKING_MANAGER: first device may be approved; later unknown devices become PENDING.
   - STAFF: unknown device becomes PENDING and login is rejected with DEVICE_NOT_TRUST.
8. For STAFF, require approved device, kiosk binding, active kiosk, same tenant, and active kiosk_staff assignment.
9. Create session row with refresh JTI and expiry.
10. Generate access token and refresh token.
11. Return access token in body and refresh_token HttpOnly cookie.
```

Database reads: `users`, `tenants`, `roles`, `devices`, `kiosk`, `kiosk_staff`.

Database writes: `devices` for new/pending/approved device; `sessions` for login.

Success result: authenticated response with JWT pair.

Failure cases: invalid credentials, inactive user/tenant, untrusted device, missing kiosk context, inactive kiosk, staff not assigned to kiosk.

### RefreshTokenUseCase - `POST /auth/refresh`

Actor: user with valid refresh token.

Controller/service: `AuthenticationController.refresh` -> `AuthenticationServiceImpl.refresh`.

```text
1. Read refresh token from HttpOnly cookie or X-Refresh-Token header.
2. Reject if missing/blank.
3. Verify token and extract userId, tenantId, sessionId, refreshJti.
4. Load session by sessionId.
5. Reject if revoked, expired, or userId mismatch.
6. Atomically rotate refresh_jti from old JTI to new JTI.
7. If rotate count is not 1 -> reject as replay/invalid.
8. Generate new access token.
9. Generate new refresh token with original session absolute expiry.
10. Return token pair and replacement refresh cookie.
```

Database reads: `sessions`.

Database writes: `sessions.refresh_jti`.

Success result: new access token and rotated refresh token.

Failure cases: missing token, invalid token, expired/revoked session, replayed refresh token.

### GetCurrentProfileUseCase - `GET /auth/me`

Actor: authenticated user.

Controller/service: `AuthenticationController.getMyProfile` -> `AuthenticationServiceImpl.getMyProfile`.

```text
1. Read JWT from security context.
2. Extract userId, tenantId, sessionId.
3. Resolve session authorization from Redis cache or DB.
4. Load user profile.
5. If role contains STAFF, resolve workContext from session/user/tenant.
6. Return id, tenantId, username, fullName, phone, roles, permissions, optional kiosk/parking context.
```

Database reads: `users`, `sessions`, `roles`, `permissions`, `kiosk`, `kiosk_staff`.

Database writes: none.

Failure cases: unauthenticated, revoked session, user not found.

### CreateTenantFirstManagerUseCase - `POST /admin/tenants`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminTenantManagementController.provisionTenant` -> `AdminTenantManagementServiceImpl.provisionTenant`.

```text
1. Validate tenant/company fields, manager username/email, and initial password.
2. Normalize tenant slug and manager username.
3. Reject duplicate tenant slug or manager username.
4. Create tenant with ACTIVE status.
5. Create first PARKING_MANAGER user under the tenant.
6. Hash initial manager password.
7. Assign PARKING_MANAGER role.
8. Return tenant summary with manager information.
```

Database reads: `tenants`, `users`, `roles`.

Database writes: `tenants`, `users`, `user_roles`.

Failure cases: duplicate slug/username, missing role, invalid password/body.

### ListUpdateTenantUseCase - `GET /admin/tenants`, `PATCH /admin/tenants/{id}/status`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminTenantManagementController`.

```text
List:
1. Validate page and size.
2. Query tenants with summary counts.
3. Return page response.

Toggle status:
1. Load tenant by id.
2. Switch ACTIVE <-> SUSPENDED.
3. Save tenant.
4. Revoke active sessions for users under tenant.
5. Evict tenant/auth caches.
6. Return new status summary.
```

Database reads: `tenants`, `users`, `sessions`.

Database writes: `tenants.status`, `sessions.revoked_at`, cache entries.

Failure cases: tenant not found, invalid page/size, unauthorized role.

### GetPermissionTreeUseCase - `GET /admin/permissions/tree`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminPermissionController.getPermissionTree`.

```text
1. Load active permission definitions.
2. Group by scope, module, resource, label, and action.
3. Return UI-friendly tree.
```

Database reads: `permissions`.

Database writes: none.

Failure cases: unauthenticated or non-admin.

### UpdateRolePermissionsUseCase - `PUT /admin/roles/{roleId}/permissions`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminPermissionController.updateRolePermissions`.

```text
1. Load role by roleId.
2. Validate all requested permissionIds exist and are active.
3. Delete existing role_permission rows for role.
4. Insert new role_permission rows.
5. Evict session authorization cache.
6. Return counts/role summary.
```

Database reads: `roles`, `permissions`, `role_permissions`.

Database writes: `role_permissions`; cache eviction.

Failure cases: role not found, missing permission IDs, invalid body.

### GetSystemHealthUseCase - `GET /admin/system-health/*`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminSystemHealthController` -> `AdminSystemHealthServiceImpl`.

```text
1. For summary, aggregate request counts, errors, and recent health metrics.
2. For services, check database, Redis, and MinIO where configured.
3. For traffic/top-endpoints/errors, query api_traffic_logs by date range.
4. Return dashboard DTOs.
```

Database reads: `api_traffic_logs`; dependency health checks.

Database writes: none.

Failure cases: invalid date range/granularity/limit, unauthorized.

### GetAuditLogsUseCase - `GET /admin/audit/logs`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminAuditSecurityController.getAuditLogs`.

```text
1. Validate optional actor, role, severity, from/to, page, and size.
2. Query audit logs using filters.
3. Map actor/user/tenant fields.
4. Return paginated result.
```

Database reads: `audit_logs`, `users`, `tenants`.

Database writes: none.

Failure cases: invalid filters, unauthorized.

### RevokeAdminSessionDeviceUseCase - `POST /admin/sessions/{id}/revoke`, `POST /admin/devices/{id}/revoke`

Actor: `SYSTEM_ADMIN`.

Controller/service: `AdminAuditSecurityController`.

```text
Session revoke:
1. Load session by id.
2. If active, set revoked_at.
3. Add access-token/session deny marker to cache where applicable.
4. Write audit log with reason.
5. Return revoked session response.

Device revoke:
1. Load device by id.
2. Set device status SUSPENDED.
3. Revoke active sessions associated with device.
4. Write audit log with reason.
5. Return revoked device response.
```

Database reads: `sessions`, `devices`, `users`, `tenants`.

Database writes: `sessions.revoked_at`, `devices.status`, `audit_logs`, cache.

Failure cases: session/device not found, wrong role, invalid reason body.

## Group 2 - Parking Facility & Daily Operations

### CreateParkingUseCase - `POST /manager/parkings`

Actor: `PARKING_MANAGER`.

Controller/service: `ManagerFacilityController.createParking` -> `ManagerFacilityServiceImpl.createParking`.

```text
1. Resolve tenant from manager JWT.
2. Normalize parking code.
3. Reject duplicate parking code in tenant.
4. Create parking with address, description, capacity, and status.
5. Save and return parking response.
```

Reads: `parkings`, `tenants`. Writes: `parkings`.

Failure cases: duplicate code, invalid fields, wrong role.

### CreateFloorUseCase - `POST /manager/parkings/{parkingId}/floors`

```text
1. Resolve tenant.
2. Load parking by id and tenant.
3. Normalize floor code.
4. Reject duplicate floor code under parking.
5. Create floor with display order and active flag.
6. Save and return floor response.
```

Reads: `parkings`, `floors`. Writes: `floors`.

Failure cases: parking not found, duplicate floor code, invalid display order.

### UploadUpdateFloorMapUseCase - `PATCH /manager/floors/{id}/map`

```text
1. Resolve tenant.
2. Load floor by id and tenant.
3. Validate map image URL/object reference.
4. Update floor.map_image_url.
5. Return map response.
```

Reads: `floors`. Writes: `floors.map_image_url`.

Failure cases: floor not found, invalid map reference.

### CreateZoneUseCase - `POST /manager/floors/{floorId}/zones`

```text
1. Resolve tenant.
2. Load floor and parking.
3. Normalize zone code.
4. Reject duplicate zone code under parking.
5. If vehicleTypeId is present, load active vehicle type.
6. Create zone with capacity, status, floor, parking, vehicle type.
7. Save and return zone response.
```

Reads: `floors`, `parkings`, `zones`, `vehicle_types`. Writes: `zones`.

Failure cases: duplicate code, inactive vehicle type, floor not found.

### CreateSlotUseCase - `POST /manager/zones/{zoneId}/slots`

```text
1. Resolve tenant.
2. Load zone with parking/floor.
3. Normalize slot code.
4. Reject duplicate slot code in zone.
5. Create slot with code, slotNumber, status, optional coordinates.
6. Save and return slot response.
```

Reads: `zones`, `slots`. Writes: `slots`.

Failure cases: duplicate slot code, zone not found, invalid coordinates/status.

### ImportSlotsUseCase - `POST /manager/slots/import`

```text
1. Validate multipart Excel file is present and non-empty.
2. Parse workbook header row.
3. Require expected columns.
4. Resolve referenced parking/floor/zone codes.
5. Validate each row: zone match, slot code, duplicate slot code, status, coordinates.
6. Insert valid slot rows.
7. Return imported/failed row counts and row errors.
```

Reads: `parkings`, `floors`, `zones`, `slots`. Writes: `slots`.

Failure cases: missing file/header, bad row data, duplicate slot.

### UpdateSlotCoordinateUseCase - `PATCH /manager/slots/{id}/coordinate`

```text
1. Resolve tenant.
2. Load slot by id and tenant.
3. Validate x/y percentage coordinates.
4. Update slot.x_coordinate and slot.y_coordinate.
5. Return coordinate response.
```

Reads: `slots`. Writes: `slots.x_coordinate`, `slots.y_coordinate`.

Failure cases: slot not found, invalid coordinate.

### GenerateRfidCardsUseCase - `POST /manager/rfid-cards/generate`

```text
1. Resolve tenant.
2. Validate prefix/count/range request.
3. Generate unique card codes.
4. Generate unique QR tokens.
5. Insert RFID cards with ACTIVE or requested status.
6. Return created count and card range.
```

Reads: `rfid_cards`. Writes: `rfid_cards`.

Failure cases: invalid count/prefix, duplicate code/token.

### CreateStaffUseCase - `POST /manager/staff`

```text
1. Resolve tenant and manager user id.
2. Normalize username.
3. Reject duplicate username.
4. Load STAFF role.
5. Hash initial password.
6. Create user under tenant with status.
7. Insert user_roles row for STAFF.
8. Return staff response without password.
```

Reads: `users`, `roles`. Writes: `users`, `user_roles`.

Failure cases: duplicate username, missing password, missing STAFF role.

### CreateKioskUseCase - `POST /manager/kiosks`

```text
1. Resolve tenant.
2. Load parking by id and tenant.
3. Generate/normalize kiosk code.
4. Reject duplicate kiosk code under parking.
5. Create kiosk with type ENTRY, EXIT, or ENTRY_EXIT and status.
6. Return kiosk response.
```

Reads: `parkings`, `kiosk`. Writes: `kiosk`.

Failure cases: parking not found, duplicate code, invalid kiosk type/status.

### AssignStaffToKioskUseCase - `POST /manager/kiosks/{id}/staff/{staffId}`

```text
1. Resolve tenant.
2. Load kiosk by id and tenant.
3. Load staff user under tenant and verify STAFF role.
4. If active assignment already exists, return or reject according to service rule.
5. Create kiosk_staff assignment with assigned_at and active flag.
6. Return assignment response.
```

Reads: `kiosk`, `users`, `user_roles`, `kiosk_staff`. Writes: `kiosk_staff`.

Failure cases: kiosk/staff not found, staff not in tenant, duplicate assignment.

### StaffLoginWithDeviceTrustUseCase - `POST /auth/login`

Same endpoint as LoginUseCase, but the staff branch is important for Group 2:

```text
1. Staff enters username/password/deviceFingerprint.
2. Unknown device creates PENDING devices row and login fails with DEVICE_NOT_TRUST.
3. Manager sees pending device approval.
4. After approval, login requires device.kiosk_id.
5. Kiosk must be ACTIVE.
6. Staff must be assigned to kiosk in kiosk_staff.
7. Token is issued and /auth/me includes staff workContext.
```

Reads: `users`, `devices`, `kiosk`, `kiosk_staff`, `sessions`. Writes: `devices`, `sessions`.

### ApproveStaffDeviceUseCase - `POST /manager/device-approvals/{id}/approve`

```text
1. Resolve manager tenant and manager user id.
2. Load pending device by id under tenant.
3. Load kiosk by request.kioskId under tenant.
4. Validate kiosk is active and belongs to tenant.
5. Set device APPROVED, approvedBy, approvedAt, expiresAt, kiosk.
6. Save and return device response.
```

Reads: `devices`, `kiosk`. Writes: `devices`.

Failure cases: device/kiosk not found, invalid device status, inactive kiosk.

### StaffCheckInParkingSessionUseCase - `POST /staff/parking-sessions/check-in`

```text
1. Resolve tenant from staff JWT and trusted kiosk context.
2. Resolve parkingId from request or staff kiosk context.
3. Load parking by tenant.
4. Load RFID card by cardCode.
5. Require card status ACTIVE.
6. Reject if card already has ACTIVE parking session.
7. Find first AVAILABLE slot under parking.
8. Resolve vehicle type from request or zone default.
9. Create parking_session with ACTIVE status, plate, card, slot, zone, parking, vehicle type.
10. Set slot status OCCUPIED.
11. Return sessionId, assigned slot, cardCode, qrToken, and PWA path.
```

Reads: `parkings`, `rfid_cards`, `parking_sessions`, `slots`, `zones`, `vehicle_types`.

Writes: `parking_sessions`, `slots.status`.

Failure cases: no card, card not active, card already in use, no available slot, inactive vehicle type.

### GetPwaActiveSessionByCardQrUseCase - `GET /pwa/cards/{qrToken}/active-session`

```text
1. Normalize qrToken.
2. Load RFID card by qrToken.
3. Require card ACTIVE.
4. Find ACTIVE parking session for card.
5. Load assigned parking, floor, zone, slot, and map data.
6. Return guide: plate, check-in time, parking/floor/zone/slot names, map URL, slot coordinates.
```

Reads: `rfid_cards`, `parking_sessions`, `parkings`, `floors`, `zones`, `slots`.

Writes: none.

Failure cases: blank token, inactive card, no active session.

### StaffExitPreviewUseCase - `POST /staff/parking-sessions/exit-preview`

```text
1. Resolve tenant and staff workContext.
2. Require kiosk type EXIT or ENTRY_EXIT.
3. Load RFID card by cardCode.
4. Find ACTIVE parking session for card.
5. Ensure session parking matches kiosk parking.
6. Calculate quote from pricing rule and session checkInAt.
7. Decide:
   - paid and within exitDeadline -> ALLOW_EXIT.
   - unpaid -> COLLECT_CASH.
   - paid but grace expired -> GRACE_EXPIRED_SURCHARGE.
8. Return amount, surcharge, payment status, and decision.
```

Reads: `rfid_cards`, `parking_sessions`, `pricing_rules`, `kiosk`, `kiosk_staff`.

Writes: none.

Failure cases: wrong kiosk type, no active session, missing pricing rule, session not in kiosk parking.

### StaffCompleteExitUseCase - `POST /staff/parking-sessions/complete-exit`

```text
1. Resolve tenant and require EXIT/ENTRY_EXIT kiosk context.
2. Load parking session by sessionId.
3. Reject if already COMPLETED or not ACTIVE.
4. Verify cardCode matches session card.
5. Validate session parking equals kiosk parking.
6. Rebuild exit preview to prevent stale client decisions.
7. Validate paymentMode:
   - ONLINE only when allowed and collectedAmount is zero.
   - CASH requires enough collectedAmount.
   - SURCHARGE requires enough surcharge collection.
8. Update payment fields for cash/surcharge if needed.
9. Set checkOutAt and session status COMPLETED.
10. Set slot status AVAILABLE.
11. Save session and slot.
12. Return completed status, slot status, card status, and message.
```

Reads: `parking_sessions`, `rfid_cards`, `slots`, `pricing_rules`, `kiosk`.

Writes: `parking_sessions`, `slots.status`.

Failure cases: wrong payment mode, insufficient cash, duplicate completion, wrong card, wrong kiosk parking.

### GetAvailableRfidCardsUseCase - `GET /staff/rfid-cards/available`

```text
1. Resolve staff tenant and kiosk context.
2. Search ACTIVE RFID cards by optional search text.
3. Exclude cards already linked to ACTIVE parking sessions.
4. Limit result count.
5. Return card code and metadata for check-in.
```

Reads: `rfid_cards`, `parking_sessions`, `kiosk_staff`.

Writes: none.

Failure cases: invalid limit, missing kiosk context.

## Group 3 - Pricing, Payment & Safety Compliance

### CreatePricingRuleUseCase - `POST /manager/pricing/rules`

```text
1. Resolve tenant.
2. Load parking if parkingId is present.
3. Load vehicle type and require active.
4. Validate block minutes/prices/free minutes/daily cap/grace minutes.
5. Reject duplicate ACTIVE rule for same parking scope and vehicle type.
6. Create pricing_rule with status ACTIVE unless request overrides.
7. Return pricing rule response.
```

Reads: `parkings`, `vehicle_types`, `pricing_rules`. Writes: `pricing_rules`.

Failure cases: duplicate active scope, inactive vehicle type, invalid price config.

### PreviewPricingRuleUseCase - `POST /manager/pricing/rules/{id}/preview`

```text
1. Resolve tenant.
2. Load pricing rule by id and tenant.
3. Validate vehicleTypeId matches rule if provided.
4. Require checkOutAt after checkInAt.
5. Calculate free minutes, first block, next blocks, daily cap, total amount.
6. Return breakdown without writing DB state.
```

Reads: `pricing_rules`. Writes: none.

Failure cases: rule not found, invalid time range, vehicle type mismatch.

### GetPwaCheckoutQuoteUseCase - `GET /pwa/cards/{qrToken}/checkout-quote`

```text
1. Normalize QR token.
2. Load active RFID card.
3. Load ACTIVE parking session for card.
4. Calculate quote from session checkInAt to now.
5. Include payment status, paidAt, exitDeadline, and nextAction.
6. Return quote to Driver PWA.
```

Reads: `rfid_cards`, `parking_sessions`, `pricing_rules`.

Writes: none.

Failure cases: card inactive, no active session, pricing missing.

### CreatePayosPaymentIntentUseCase - `POST /pwa/cards/{qrToken}/payment-intents`

```text
1. Load active card by QR token.
2. Load ACTIVE parking session for card.
3. If session already PAID, return paid response.
4. Calculate current quote.
5. Search for reusable PENDING payment intent for same session and amount.
6. If reusable and unexpired, return existing intent response.
7. Load pricing rule from quote.
8. If amount > 0, require PayOS payment creation config.
9. Generate unique orderCode.
10. Create payment_intent with PENDING status and quote snapshot.
11. If amount is zero:
    - mark intent PAID.
    - mark parking session PAID.
    - set exitDeadline from grace minutes.
12. If amount > 0:
    - call PayOS create payment link.
    - store checkoutUrl, qrCode, providerPaymentLinkId, raw provider response.
13. Return payment intent response.
```

Reads: `rfid_cards`, `parking_sessions`, `pricing_rules`, `payment_intents`.

Writes: `payment_intents`; optionally `parking_sessions.payment_status`, `paid_at`, `exit_deadline`.

Failure cases: provider disabled, no active session, pricing missing, provider error.

### PayosWebhookProcessingUseCase - `POST /payments/webhooks/payos`

```text
1. Receive PayOS JSON payload.
2. Extract data, signature, orderCode, event code.
3. Save payment_webhook_log with RECEIVED status and raw payload.
4. Verify PayOS checksum signature.
5. If invalid, mark log FAILED and reject.
6. Require orderCode.
7. Load payment_intent by orderCode.
8. If not found, mark log IGNORED and return success.
9. If intent already PAID, mark log PROCESSED and return success.
10. Validate paid amount equals intent amount.
11. If event indicates paid:
    - set intent status PAID and paidAt.
    - set parking_session payment status PAID.
    - set payment method/reference/totalAmount/paidAt.
    - set exitDeadline using pricing rule grace minutes.
    - mark webhook log PROCESSED.
12. If not a paid event, mark log IGNORED.
13. Return success response.
```

Reads: `payment_intents`, `pricing_rules`, `parking_sessions`.

Writes: `payment_webhook_logs`, `payment_intents`, `parking_sessions`.

Failure cases: invalid signature, missing order code, amount mismatch, malformed payload.

### GetPaymentIntentStatusUseCase - `GET /pwa/payment-intents/{orderCode}`

```text
1. Load payment_intent status summary by orderCode.
2. Resolve live status: PENDING with expired expiresAt becomes EXPIRED in response.
3. Include session, card, amount, checkoutUrl, qrCode, paidAt, and exitDeadline.
4. Return public status response.
```

Reads: `payment_intents`, `parking_sessions`, `rfid_cards`.

Writes: none.

Failure cases: payment intent not found.

### CreateFireExtinguisherUseCase - `POST /manager/fire-extinguishers`

```text
1. Resolve tenant.
2. Normalize extinguisher code.
3. Reject duplicate code in tenant.
4. Load parking and floor; require floor belongs to parking.
5. If zoneId is present, load zone and require zone belongs to floor.
6. Validate coordinates, dates, type, and status.
7. Create fire_extinguisher with location, dates, status, and map pin.
8. Return extinguisher response.
```

Reads: `parkings`, `floors`, `zones`, `fire_extinguishers`.

Writes: `fire_extinguishers`.

Failure cases: duplicate code, invalid coordinate, parking/floor/zone mismatch.

### UpdateExtinguisherCoordinateUseCase - `PATCH /manager/fire-extinguishers/{id}/coordinate`

```text
1. Resolve tenant.
2. Load extinguisher by id and tenant.
3. Validate x/y coordinates are between 0 and 100.
4. Update x_coordinate and y_coordinate.
5. Return response.
```

Reads: `fire_extinguishers`. Writes: `fire_extinguishers.x_coordinate`, `y_coordinate`.

### GetFireSafetyMapUseCase - `GET /manager/floors/{floorId}/fire-safety-map`

```text
1. Resolve tenant.
2. Load floor and map image.
3. Load non-deleted extinguishers on floor.
4. Build pin list with code, type, status, coordinates, inspection due data.
5. Return floor fire-safety map response.
```

Reads: `floors`, `fire_extinguishers`.

Writes: none.

### GetInspectionLogsUseCase - `GET /manager/fire-inspections/logs`

```text
1. Resolve tenant.
2. Apply optional extinguisher, parking, floor, result, and date filters.
3. Query inspection logs with extinguisher and staff data.
4. If photo_object_key exists, include photo display/download URL when service supports it.
5. Return paginated logs.
```

Reads: `fire_extinguisher_inspections`, `fire_extinguishers`, `users`, storage for display URLs.

Writes: none.

### StaffGetDueFireInspectionsUseCase - `GET /staff/fire-inspections/due`

```text
1. Resolve staff tenant and trusted kiosk context.
2. Use kiosk parking as allowed inspection scope.
3. Apply optional floor/status filters.
4. Return extinguishers due for inspection or matching filter.
```

Reads: `kiosk`, `kiosk_staff`, `fire_extinguishers`.

Writes: none.

Failure cases: missing kiosk context, forbidden parking scope.

### StaffSubmitFireInspectionUseCase - `POST /staff/fire-inspections`

```text
1. Resolve staff tenant, staff user id, and kiosk context.
2. Load fire extinguisher by id and tenant.
3. Require extinguisher parking equals kiosk parking.
4. Validate checklist booleans, result, nextInspectionAt, note, and optional photoObjectKey.
5. Create fire_extinguisher_inspection row with staff, result, checklist, photo URL/object key.
6. Update extinguisher lastInspectedAt.
7. Update extinguisher nextInspectionAt.
8. Update extinguisher status based on inspection result.
9. Return inspection response.
```

Reads: `fire_extinguishers`, `kiosk`, `kiosk_staff`, `users`.

Writes: `fire_extinguisher_inspections`, `fire_extinguishers`.

Failure cases: extinguisher not found, not in kiosk parking, invalid photo object key.

### StaffPresignFireInspectionPhotoUploadUseCase - `POST /staff/fire-inspections/photos/presign-upload`

```text
1. Resolve staff tenant and trusted kiosk context.
2. Validate fileName is present.
3. Validate contentType is an allowed image type.
4. Validate file extension matches content type.
5. Create tenant-scoped object key under fire-inspections folder.
6. Ask storage service for presigned PUT URL.
7. Return uploadUrl, method, headers, objectKey, publicUrl, and expiry.
8. Client uploads file directly to MinIO/S3 using the returned URL.
9. Client submits objectKey in StaffSubmitFireInspectionUseCase.
```

Reads: `kiosk`, `kiosk_staff`; storage configuration.

Writes: no database row at presign time; object is written by client to storage.

Failure cases: invalid content type/extension, storage unavailable, missing kiosk context.
