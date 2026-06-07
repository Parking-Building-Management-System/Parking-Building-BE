# Backend Demo Script

This script supports two modes: UI demo first, Bruno/API demo as fallback or technical proof.

Do not show real JWTs, refresh tokens, PayOS keys, MinIO keys, passwords, or webhook signatures.

## Demo Prerequisites

- Backend running on local port `8081` or production demo URL.
- Bruno collection opened from `bruno/SmartPark`.
- Environment selected: `Local` or a safe production copy.
- Demo seed data or prepared IDs:
  - `parkingId`
  - `floorId`
  - `zoneId`
  - `slotId`
  - `cardCode`
  - `vehicleTypeId`
  - `staffId`
  - `kioskId`
  - `exitKioskId`
  - `pricingRuleId`
  - `fireExtinguisherId`

## UI Demo Flow

### 1. System Admin/Manager Login

Action:

- Login as System Admin or Parking Manager.
- Show that the profile includes role and tenant context.

Talk track:

- "The backend uses JWT and role-based access. Staff also needs trusted device and kiosk context."

Expected result:

- Dashboard loads.
- User role is visible.

### 2. Manager Checks Facility Setup

Action:

- Open parking/floor/zone/slot setup.
- Show parking topology.

Talk track:

- "Facility data is hierarchical: parking -> floor -> zone -> slot."

Expected result:

- Parking, floors, zones, and slots are visible.

### 3. Manager Checks Map/Slot Pins

Action:

- Open floor map.
- Show slot pins.

Talk track:

- "Map pins come from slot coordinate APIs. The driver PWA reuses the same map data."

Expected result:

- Floor map image and slot coordinates display.

### 4. Manager Checks RFID Cards

Action:

- Open RFID card list.
- Pick an active card not in use.

Talk track:

- "RFID cards identify parking sessions and contain QR tokens for the PWA."

Expected result:

- Active card available for entry.

### 5. Staff Entry Login and Check-In

Action:

- Login staff from approved entry kiosk device.
- If login fails with `DEVICE_NOT_TRUST`, switch to manager and approve the pending device.
- Run check-in with plate and `cardCode`.

Talk track:

- "Staff cannot operate from any random device. Device approval and kiosk assignment are required."

Expected result:

- New active session.
- Assigned slot.
- QR token/PWA path returned.

### 6. Driver PWA QR Opens Assigned Slot Map

Action:

- Open QR/PWA path from check-in response.
- Show parking/floor/zone/slot and map pin.

Talk track:

- "The driver does not need an account. The QR token resolves only the active card session."

Expected result:

- Assigned slot guide is displayed.

### 7. Driver Checkout Quote

Action:

- Open checkout quote.

Talk track:

- "The quote is calculated from check-in time, vehicle type, and active pricing rule."

Expected result:

- Amount, duration, payment status, and next action are visible.

### 8. PayOS Payment Intent

Action:

- Create payment intent.
- If real PayOS is configured, open checkout URL.
- Otherwise explain provider-disabled/sample mode.

Talk track:

- "Payment intent stores the quote snapshot and order code. A real PayOS webhook marks the session paid."

Expected result:

- Payment order code, checkout URL or provider-disabled error.

### 9. Staff Exit Preview/Complete

Action:

- Login exit staff from approved exit or mixed kiosk.
- Preview exit using card code.
- Complete online, cash, or surcharge exit based on decision.

Talk track:

- "Exit preview recalculates the current decision. Complete exit releases the slot."

Expected result:

- Session completed.
- Slot status is `AVAILABLE`.

### 10. Fire Safety Manager Map

Action:

- Manager opens fire extinguisher list/summary.
- Open fire-safety map for a floor.

Talk track:

- "Fire Safety uses the same floor-map concept, but pins represent extinguishers and inspection status."

Expected result:

- Fire extinguisher pins visible.

### 11. Staff Fire Inspection/Photo Upload

Action:

- Staff opens due inspections.
- Presign photo upload.
- Upload image manually/browser if UI supports it.
- Submit inspection with `photoObjectKey`.

Talk track:

- "The backend creates a safe upload URL and stores only the object key in the inspection log."

Expected result:

- Inspection submitted.
- Extinguisher last/next inspection updated.

### 12. Manager Inspection Logs

Action:

- Manager opens inspection logs.
- Filter by result/date if useful.
- Show photo display link when present.

Talk track:

- "Managers can review inspection evidence and compliance history."

Expected result:

- New inspection log visible.

## API/Bruno Demo Flow

Use the same story through `bruno/SmartPark`.

### 1. Login

Folder/request:

- `00 Auth / 01 Login System Admin`
- `00 Auth / 02 Login Manager`
- `00 Auth / 03 Login Staff`
- `00 Auth / 04 Get Me`

Variables to copy:

- `systemAdminToken`
- `managerToken`
- `staffEntryToken`
- `staffExitToken`

Expected output:

- Login returns `accessToken`.
- `Get Me` returns roles and staff `workContext` when logged in as staff.

### 2. Manager Facility

Folder:

- `02 Manager Facility`

Requests:

- `01 List Parkings`
- `02 Create Parking`
- `07 List Floors By Parking`
- `08 Create Floor`
- `11 List Zones By Floor`
- `12 Create Zone`
- `16 Create Slot`
- `22 Get Floor Map`
- `23 Update Floor Map`
- `24 Update Slot Coordinate`
- `28 List RFID Cards`
- `29 Generate RFID Cards`

Variables to copy:

- `parkingId`
- `floorId`
- `zoneId`
- `slotId`
- `cardCode`
- `qrToken` when available

Expected output:

- Facility IDs are returned.
- Slot map includes coordinates.
- RFID cards have codes and QR tokens.

### 3. Manager Staff and Devices

Folder:

- `03 Manager Staff Devices`

Requests:

- `02 Create Staff`
- `08 Create Entry Kiosk`
- `09 Create Exit Kiosk`
- `15 Assign Staff To Kiosk`
- `17 List Device Approvals`
- `18 Approve Device With Kiosk`

Variables to copy:

- `staffId`
- `kioskId`
- `exitKioskId`
- `deviceApprovalId`
- `deviceId`

Expected output:

- Staff account created.
- Kiosk created.
- Device approval becomes `APPROVED`.

### 4. Staff Entry

Folder:

- `04 Staff Entry`

Requests:

- `01 Staff Get Me`
- `02 Check In Parking Session`

Variables to copy:

- `sessionId`
- `qrToken`
- `cardCode`

Expected output:

- Check-in response includes assigned slot and PWA path.

### 5. Driver PWA

Folder:

- `05 PWA Driver`

Requests:

- `01 Get Active Session By QR Token`
- `02 Get Checkout Quote`

Expected output:

- Active session shows slot/map guide.
- Checkout quote shows amount and next action.

### 6. Pricing and Payment

Folders:

- `06 Manager Pricing`
- `07 PWA Payment PayOS`

Requests:

- `02 Create Pricing Rule`
- `07 Preview Pricing Rule`
- `01 Create Payment Intent`
- `02 Get Payment Intent Status`
- `03 PayOS Webhook Sample`

Variables to copy:

- `pricingRuleId`
- `paymentOrderCode`

Expected output:

- Pricing preview returns amount/breakdown.
- Payment intent returns order code and checkout URL if PayOS is configured.
- Sample webhook should fail signature verification unless replaced with a real signed payload.

### 7. Staff Exit

Folder:

- `08 Staff Exit Gate`

Requests:

- `01 Exit Preview`
- `02 Complete Online Exit`
- `03 Complete Cash Exit`
- `04 Complete Surcharge Exit`

Expected output:

- Preview returns decision.
- Complete exit returns `COMPLETED`, slot `AVAILABLE`.

### 8. Fire Safety

Folder:

- `10 Fire Safety`

Requests:

- `02 Manager Create Fire Extinguisher`
- `06 Manager Update Fire Extinguisher Coordinate`
- `08 Manager Fire Safety Map`
- `10 Staff Due Fire Inspections`
- `16 Staff Presign Inspection Photo Upload`
- `17 Staff Submit Inspection With Photo Object Key`
- `18 Manager Logs With Photo Display URL`

Variables to copy:

- `fireExtinguisherId`
- `inspectionPhotoObjectKey`

Expected output:

- Map returns extinguisher pin.
- Presign returns upload URL and object key.
- Inspection log includes result and optional photo key/display URL.

## Common Demo Issues

- `DEVICE_NOT_TRUST`: manager must approve the staff device and bind it to kiosk.
- Staff exit requires `EXIT` or `ENTRY_EXIT` kiosk. ENTRY-only kiosk is rejected.
- PayOS webhook requires a public backend URL and real PayOS credentials/checksum key.
- The sample PayOS webhook in Bruno intentionally has an invalid signature.
- MinIO upload from browser requires storage environment and CORS.
- PWA routes are public and QR-token based, not bearer-authenticated.
- If quote fails with `PRICING_RULE_NOT_CONFIGURED`, create or activate a pricing rule for the parking/vehicle type.
- If no slot is available, release a previous demo session or create another available slot.
