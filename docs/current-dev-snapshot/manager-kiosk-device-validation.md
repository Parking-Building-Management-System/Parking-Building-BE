# Manager Kiosk / Device / Staff Work Context Validation

Validation date: 2026-05-26

Base URL: `http://localhost:8081`

Tokens used only as local shell variables. Full access and refresh tokens are intentionally omitted.

## Endpoint Sequence Tested

1. `GET /auth/me`
2. `GET /manager/parkings`
3. `GET /manager/staff`
4. `POST /manager/kiosks`
5. `POST /manager/kiosks/{kioskId}/staff/{staffId}`
6. `GET /manager/kiosks/{kioskId}/staff`
7. `POST /manager/staff/{staffId}/reset-password`
8. `POST /auth/login`
9. `GET /manager/device-approvals`
10. `POST /manager/device-approvals/{approvalId}/approve`
11. `GET /auth/me` as STAFF
12. `POST /staff/parking-sessions/check-in` without `parkingId`
13. Negative: approve device to kiosk where staff is not assigned
14. Negative: staff login with approved device while kiosk is inactive
15. Negative: check-in without `parkingId` when kiosk context is invalid

## IDs Used

- Tenant: `7648d385-58f3-ddda-fc39-6bce27cff0fd`
- Manager username: `manager@bcons.smartpark.local`
- Parking: `f0f638cb-7a99-61ba-15ad-22025aac3f01`
- Parking name: `Bcons Plaza Residential Parking`
- Positive staff: `4a1865ee-341c-9e0c-c7d8-45d5e406ee4f`
- Positive staff username: `staff01@bcons-plaza.smartpark.local`
- Validation kiosk: `e2c3f878-fba8-421c-8d47-ac130ef0c648`
- Negative unassigned staff: `9349b4b0-0412-1a87-45f4-499ecb94d203`
- Negative unassigned staff username: `staff02@bcons-plaza.smartpark.local`
- Positive device approval: `019e63da-241c-7ba7-bb59-8ce427fb76c8`
- Negative approval: `019e6402-73e9-7a33-901c-f85b35f61882`
- Check-in session created: `84038740-d6d3-4e7b-b3ea-381ada6b5a46`
- Check-in RFID card: `BCONS-0003`

## Redacted Curl Samples

Manager auth header:

```bash
AUTH="Authorization: Bearer <redacted-manager-access-token>"
```

Create kiosk:

```bash
curl -X POST "$BASE_URL/manager/kiosks" \
  -H "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "parkingId": "f0f638cb-7a99-61ba-15ad-22025aac3f01",
    "name": "Bốt vào B1 - Validation",
    "type": "ENTRY",
    "status": "ACTIVE"
  }'
```

Approve staff device:

```bash
curl -X POST "$BASE_URL/manager/device-approvals/<approval-id>/approve" \
  -H "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "kioskId": "e2c3f878-fba8-421c-8d47-ac130ef0c648",
    "expiresAt": null
  }'
```

Permanent approval uses `expiresAt: null`. Temporary approval must provide a future ISO timestamp:

```bash
curl -X POST "$BASE_URL/manager/device-approvals/<approval-id>/approve" \
  -H "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "kioskId": "e2c3f878-fba8-421c-8d47-ac130ef0c648",
    "expiresAt": "2026-05-27T10:00:00"
  }'
```

Staff login:

```bash
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "staff01@bcons-plaza.smartpark.local",
    "password": "Password@123",
    "deviceFingerprint": "validation-fingerprint-002",
    "deviceLabel": "Validation Browser Device"
  }'
```

Staff check-in without `parkingId`:

```bash
curl -X POST "$BASE_URL/staff/parking-sessions/check-in" \
  -H "Authorization: Bearer <redacted-staff-access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "plateNumber": "TEST-VALIDATION-001",
    "cardCode": "BCONS-0003",
    "entryImageUrl": "https://example.com/entry/validation.jpg"
  }'
```

## Expected vs Actual

| Step | Expected | Actual | Result |
|---|---|---|---|
| Manager `/auth/me` | `PARKING_MANAGER`, Bcons tenant | HTTP 200, `PARKING_MANAGER`, tenant `7648d385-58f3-ddda-fc39-6bce27cff0fd` | PASS |
| List parkings | Bcons parking available | HTTP 200, selected `Bcons Plaza Residential Parking` | PASS |
| List staff | Active staff available | HTTP 200, selected `staff01@bcons-plaza.smartpark.local` | PASS |
| Create kiosk | Kiosk created with selected parking | HTTP 200, `type=ENTRY`, parking matched | PASS |
| Assign staff twice | Idempotent success | HTTP 200 both calls, one assignment listed | PASS |
| List kiosk after assignment | Kiosk list includes active assignment count | `assignedStaffCount` is populated from active `kiosk_staff` rows | PASS |
| First staff login | Rejected, pending device created | Initially rejected but pending device rolled back; fixed and retested HTTP 403 with pending device present | PASS after fix |
| Pending approvals | Device appears | Device `validation-fingerprint-002` listed | PASS |
| Approve device | Approved and bound to kiosk | HTTP 200, `status=APPROVED`, kiosk matched | PASS |
| Approve with `expiresAt: null` | Permanent approval | Stores `expiresAt = null` | PASS |
| Approve with future `expiresAt` | Temporary approval succeeds | Unit covered by `ManagerDeviceApprovalServiceImplTest` | PASS |
| Approve with past `expiresAt` | Reject clear validation error | Unit covered; rejects `DEVICE_APPROVAL_EXPIRES_AT_MUST_BE_FUTURE` | PASS |
| Staff login after approval | Login succeeds | HTTP 200, access token returned | PASS |
| Staff `/auth/me` | `STAFF` with workContext | HTTP 200, kiosk/parking context returned | PASS |
| Check-in without `parkingId` | Parking resolved from kiosk | HTTP 200, session created under selected parking | PASS |
| Approve unassigned staff device | Reject with assignment error | HTTP 403, `STAFF_NOT_ASSIGNED_TO_KIOSK` | PASS |
| Inactive kiosk login | Staff cannot operate | HTTP 403, `Kiosk is not active` | PASS |
| Check-in without valid context | `KIOSK_CONTEXT_REQUIRED` | HTTP 403, `KIOSK_CONTEXT_REQUIRED` | PASS |

## Bug Found

### Pending staff device was rolled back on rejected login

Endpoint:

`POST /auth/login`

Observed before fix:

- Unknown staff device login returned `DEVICE_NOT_TRUST`.
- `Device` row was not visible in `GET /manager/device-approvals`.
- Root cause: `AuthenticationServiceImpl.authenticate()` was transactional and threw `ApiException`; Spring rolled back the pending device insert.

Fix:

```java
@Transactional(noRollbackFor = ApiException.class)
public TokenPair authenticate(AuthenticationRequest request)
```

This preserves pending device/request records while keeping login rejected.

## DB Tables Touched

- `kiosk`
- `kiosk_staff`
- `devices`
- `sessions`
- `users`
- `user_roles`
- `roles`
- `parkings`
- `rfid_cards`
- `parking_sessions`
- `slots`

## Known Limitations

- Validation created a real parking session using `BCONS-0003`; reruns should use another free Bcons RFID card if that card remains active in a session.
- The initially provided manager access token validated at the start, but later returned 401 and the provided refresh token also returned 401. Validation continued with the documented seeded Bcons manager login and did not store tokens.
- `GET /manager/device-approvals` lists pending devices only.
- `parkingId` remains accepted on staff check-in as a DEV fallback, but the validated check-in omitted it.

## Verification Commands

```bash
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

`./mvnw test` completed with build success. Testcontainers-based tests were skipped because Docker socket access is unavailable in the sandbox.

The Spring Boot app started successfully on port `8081`; Flyway reported schema version `20260526180000`.
