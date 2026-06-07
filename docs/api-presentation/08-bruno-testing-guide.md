# Bruno Testing Guide

The Bruno collection is located at:

```text
bruno/SmartPark
```

## 1. Open Collection

1. Open Bruno.
2. Choose "Open Collection".
3. Select `bruno/SmartPark`.
4. Select the `Local` environment for local backend testing.
5. Use `Production.example` only as a template. Do not put real production secrets in the repo.

Local environment default:

```text
baseUrl = http://localhost:8081
```

## 2. Start Backend

Use local port `8081` for the Bruno `Local` environment:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

If the project is built with Java 21 locally:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 3. Login and Paste Tokens

Run:

- `00 Auth / 01 Login System Admin`
- `00 Auth / 02 Login Manager`
- `00 Auth / 03 Login Staff`

Paste returned `accessToken` values into environment variables:

- `systemAdminToken`
- `managerToken`
- `staffEntryToken`
- `staffExitToken`

Do not save real JWTs or refresh tokens into committed files.

## 4. Required Variables

Core environment variables:

- `baseUrl`
- `systemAdminToken`
- `managerToken`
- `staffEntryToken`
- `staffExitToken`

Facility variables:

- `tenantId`
- `parkingId`
- `floorId`
- `zoneId`
- `slotId`
- `vehicleTypeId`

Staff/device variables:

- `staffId`
- `kioskId`
- `exitKioskId`
- `deviceApprovalId`
- `deviceId`

Parking-session variables:

- `cardCode`
- `rfidCardId`
- `qrToken`
- `sessionId`

Pricing/payment variables:

- `pricingRuleId`
- `paymentOrderCode`

Storage/fire variables:

- `objectKey`
- `inspectionPhotoObjectKey`
- `fireExtinguisherId`

Admin/security variables:

- `roleId`
- `permissionId`
- `userId`
- `sessionRevokeId`

## 5. Recommended Run Order

### Foundation/Admin

1. `00 Auth / 01 Login System Admin`
2. `00 Auth / 04 Get Me`
3. `01 System Admin / 01 Get Tenants`
4. `01 System Admin / 03 Get Vehicle Types`
5. `01 System Admin / 07 Get Permission Tree`
6. `01 System Admin / 11 Get System Health Summary`
7. `01 System Admin / 14 Get Audit Logs`

### Manager Facility

1. `00 Auth / 02 Login Manager`
2. `02 Manager Facility / 01 List Parkings`
3. `02 Manager Facility / 02 Create Parking`
4. `02 Manager Facility / 08 Create Floor`
5. `02 Manager Facility / 12 Create Zone`
6. `02 Manager Facility / 16 Create Slot`
7. `02 Manager Facility / 23 Update Floor Map`
8. `02 Manager Facility / 24 Update Slot Coordinate`
9. `02 Manager Facility / 29 Generate RFID Cards`

### Staff and Devices

1. `03 Manager Staff Devices / 02 Create Staff`
2. `03 Manager Staff Devices / 08 Create Entry Kiosk`
3. `03 Manager Staff Devices / 09 Create Exit Kiosk`
4. `03 Manager Staff Devices / 15 Assign Staff To Kiosk`
5. `00 Auth / 03 Login Staff`
6. If blocked, run `03 Manager Staff Devices / 17 List Device Approvals`
7. Run `03 Manager Staff Devices / 18 Approve Device With Kiosk`
8. Run staff login again and paste token.

### Entry and PWA

1. `04 Staff Entry / 01 Staff Get Me`
2. `10 Fire Safety / 12 Staff Available RFID Cards` or `04 Staff Entry / 02 Check In Parking Session`
3. Copy `sessionId`, `cardCode`, and `qrToken`.
4. `05 PWA Driver / 01 Get Active Session By QR Token`
5. `05 PWA Driver / 02 Get Checkout Quote`

### Pricing and Payment

1. `06 Manager Pricing / 02 Create Pricing Rule`
2. `06 Manager Pricing / 07 Preview Pricing Rule`
3. `07 PWA Payment PayOS / 01 Create Payment Intent`
4. Copy `orderCode` into `paymentOrderCode`.
5. `07 PWA Payment PayOS / 02 Get Payment Intent Status`
6. Use `07 PWA Payment PayOS / 03 PayOS Webhook Sample` only to demonstrate signature rejection, unless replaced with a real signed webhook payload.

### Exit

1. Approve/login an exit kiosk staff device and paste `staffExitToken`.
2. `08 Staff Exit Gate / 01 Exit Preview`
3. Run one matching completion request:
   - `02 Complete Online Exit`
   - `03 Complete Cash Exit`
   - `04 Complete Surcharge Exit`

### Fire Safety

1. `10 Fire Safety / 02 Manager Create Fire Extinguisher`
2. Copy `id` into `fireExtinguisherId`.
3. `10 Fire Safety / 06 Manager Update Fire Extinguisher Coordinate`
4. `10 Fire Safety / 08 Manager Fire Safety Map`
5. `10 Fire Safety / 10 Staff Due Fire Inspections`
6. `10 Fire Safety / 16 Staff Presign Inspection Photo Upload`
7. Copy `objectKey` into `inspectionPhotoObjectKey`.
8. Upload the file with the returned `uploadUrl`.
9. `10 Fire Safety / 17 Staff Submit Inspection With Photo Object Key`
10. `10 Fire Safety / 18 Manager Logs With Photo Display URL`

## 6. Fire Safety Photo Upload

Flow:

1. Run `Staff Presign Inspection Photo Upload`.
2. Copy returned `objectKey` to `inspectionPhotoObjectKey`.
3. Upload file manually with HTTP `PUT` to the returned `uploadUrl`.
4. Include required headers exactly as returned by presign response.
5. Run `Staff Submit Inspection With Photo Object Key`.
6. Manager runs inspection logs and verifies photo display/download URL.

Browser note:

- Browser upload to MinIO/S3 requires CORS configured on the storage bucket.
- If CORS is not configured, use curl/Postman/Bruno manual upload outside the API collection.

## 7. Known Limitations

- PayOS real payment requires real credentials, return/cancel URLs, and webhook configured to a public backend URL.
- Do not commit PayOS client ID, API key, checksum key, or real webhook signatures.
- MinIO upload from browser requires bucket CORS.
- Staff device must be approved before staff APIs work.
- Staff exit requires an `EXIT` or `ENTRY_EXIT` kiosk.
- PWA routes are public QR-token routes and do not use `managerToken` or `staffToken`.
- If the backend runs on another port, update `baseUrl`.

## 8. Quick Troubleshooting

- `DEVICE_NOT_TRUST`: approve the pending device in manager device approvals.
- `KIOSK_CONTEXT_REQUIRED`: approved device is not bound to a kiosk.
- `STAFF_NOT_ASSIGNED_TO_KIOSK`: assign staff to the kiosk.
- `EXIT_KIOSK_REQUIRED`: use exit staff token, not entry staff token.
- `NO_ACTIVE_SESSION_FOR_CARD`: run check-in first or use the correct `cardCode`.
- `PRICING_RULE_NOT_CONFIGURED`: create an active pricing rule for the parking and vehicle type.
- `PAYOS_INVALID_SIGNATURE`: expected for sample webhook unless using real signed data.
- `PAYMENT_PROVIDER_DISABLED`: PayOS env is not configured for payment creation.
