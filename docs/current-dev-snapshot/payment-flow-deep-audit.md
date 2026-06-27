# SmartPark Payment, Pricing, Checkout, and Staff Exit Flow Audit

This document provides a comprehensive technical audit of the checkout, payment, webhook, exit preview, and slot release flows within the SmartPark backend (`Parking-Building-BE`). This snapshot is optimized for today's demo verification.

---

## PART 1 — SWAGGER/API INVENTORY

Below is the complete inventory of all endpoints participating in the payment, check-in, checkout, and exit flows.

### 1. Manager Pricing Configuration

#### List Pricing Rules
* **HTTP Method + Path**: `GET /manager/pricing/rules`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO / Parameters**: Query parameters: `parkingId` (UUID), `vehicleTypeId` (UUID), `status` (PricingRuleStatus: `ACTIVE`/`INACTIVE`), `page` (int), `size` (int).
* **Response DTO**: `ApiResponse<PageResponse<ManagerPricingRuleResponse>>`
* **Service Called**: `ManagerPricingRuleService.getRules`
* **Repository / DB Tables Touched**: `pricing_rules` (Read), `tenants` (Read via context)
* **Business Behavior**: Fetches a paginated, tenant-filtered list of pricing rules. Allows filtering by parking building, vehicle type, and status.

#### Create Pricing Rule
* **HTTP Method + Path**: `POST /manager/pricing/rules`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO**: `ManagerPricingRuleRequest` (fields: `name`, `parkingId` [optional], `vehicleTypeId`, `freeMinutes`, `firstBlockMinutes`, `firstBlockPrice`, `nextBlockMinutes`, `nextBlockPrice`, `dailyCapPrice` [optional], `graceMinutesAfterPayment`, `status`)
* **Response DTO**: `ApiResponse<ManagerPricingRuleResponse>`
* **Service Called**: `ManagerPricingRuleService.createRule`
* **Repository / DB Tables Touched**: `pricing_rules` (Insert), `parkings` (Read reference), `vehicle_types` (Read reference), `tenants` (Read reference)
* **Business Behavior**: Saves a new pricing rule. Ensures there are no active duplicate rules for the same scope (`tenant_id`, `parking_id`, `vehicle_type_id`) using `existsActiveScope`.

#### Update Pricing Rule
* **HTTP Method + Path**: `PUT /manager/pricing/rules/{id}`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO**: `ManagerPricingRuleRequest`
* **Response DTO**: `ApiResponse<ManagerPricingRuleResponse>`
* **Service Called**: `ManagerPricingRuleService.updateRule`
* **Repository / DB Tables Touched**: `pricing_rules` (Read/Update)
* **Business Behavior**: Modifies details of an existing pricing rule. Performs duplicate checks on saving.

#### Update Pricing Rule Status
* **HTTP Method + Path**: `PATCH /manager/pricing/rules/{id}/status`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO**: `ManagerPricingRuleStatusRequest` (fields: `status`)
* **Response DTO**: `ApiResponse<ManagerPricingRuleResponse>`
* **Service Called**: `ManagerPricingRuleService.updateStatus`
* **Repository / DB Tables Touched**: `pricing_rules` (Update)
* **Business Behavior**: Toggles status between `ACTIVE` and `INACTIVE`. Validates that activating the rule does not conflict with another active rule of the same scope.

#### Soft-delete Pricing Rule
* **HTTP Method + Path**: `DELETE /manager/pricing/rules/{id}`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO**: None (Path Variable `id`)
* **Response DTO**: `ApiResponse<Void>`
* **Service Called**: `ManagerPricingRuleService.deleteRule`
* **Repository / DB Tables Touched**: `pricing_rules` (Update)
* **Business Behavior**: Sets `is_deleted = true` and `status = INACTIVE`.

#### Preview Pricing Rule Quote
* **HTTP Method + Path**: `POST /manager/pricing/rules/{id}/preview`
* **Auth Role**: `ROLE_PARKING_MANAGER` (Bearer JWT Auth)
* **Controller Class**: `ManagerPricingRuleController`
* **Request DTO**: `ManagerPricingRulePreviewRequest` (fields: `vehicleTypeId`, `checkInAt`, `checkOutAt`)
* **Response DTO**: `ApiResponse<PricingQuoteResponse>`
* **Service Called**: `ManagerPricingRuleService.preview` calling `PricingQuoteService.preview`
* **Repository / DB Tables Touched**: `pricing_rules` (Read)
* **Business Behavior**: Allows managers to dry-run pricing logic and obtain quotes based on dummy check-in/check-out inputs.

---

### 2. Driver PWA Checkout Guide & Quote

#### Resolve Active Session
* **HTTP Method + Path**: `GET /pwa/cards/{qrToken}/active-session`
* **Auth Role**: Public (No Auth Required)
* **Controller Class**: `PwaCardSessionController`
* **Request DTO**: None (Path Variable `qrToken`)
* **Response DTO**: `ApiResponse<CardActiveSessionResponse>`
* **Service Called**: `PwaCardSessionService.getActiveSession`
* **Repository / DB Tables Touched**: `rfid_cards` (Read), `parking_sessions` (Read), `slots` (Read), `zones` (Read), `floors` (Read)
* **Business Behavior**: Resolves the QR token on the card, fetches the newest `ACTIVE` session, and returns parking floor coordinates, slot codes, floor map presigned URL, and guide texts.

#### PWA Checkout Quote
* **HTTP Method + Path**: `GET /pwa/cards/{qrToken}/checkout-quote`
* **Auth Role**: Public (No Auth Required)
* **Controller Class**: `PwaCardSessionController`
* **Request DTO**: None (Path Variable `qrToken`)
* **Response DTO**: `ApiResponse<CardCheckoutQuoteResponse>`
* **Service Called**: `PwaCardSessionService.getCheckoutQuote` calling `PricingQuoteService.quote`
* **Repository / DB Tables Touched**: `rfid_cards` (Read), `parking_sessions` (Read), `pricing_rules` (Read), `payment_intents` (Read reusable pending)
* **Business Behavior**: Returns the checkout quote calculated from check-in time until now. Emits payment availability status, details of any existing pending payment intent, and suggestions for the `nextAction` (`CREATE_PAYMENT_INTENT`, `CONTINUE_PAYMENT`, `EXIT_WITHIN_GRACE_PERIOD`, `PAYMENT_PROVIDER_DISABLED`).

---

### 3. Driver PayOS Payment Intent

#### Create PayOS Payment Intent
* **HTTP Method + Path**: `POST /pwa/cards/{qrToken}/payment-intents`
* **Auth Role**: Public (No Auth Required)
* **Controller Class**: `PwaPaymentController`
* **Request DTO**: None (Path Variable `qrToken`)
* **Response DTO**: `ApiResponse<PaymentIntentResponse>`
* **Service Called**: `PwaPaymentService.createPaymentIntent`
* **Repository / DB Tables Touched**: `rfid_cards` (Read), `parking_sessions` (Read/Update), `payment_intents` (Read/Insert), `pricing_rules` (Read)
* **Business Behavior**: Creates a PayOS billing session and returns `checkoutUrl` and `qrCode`.
  * **Idempotency/Reusability**: Reuses pending, unexpired intents with the same amount.
  * **Zero Amount Handling**: If the quote is `0.00` VND, the intent is immediately set to `PAID`, and the session is marked paid with a grace period exit deadline without calling PayOS.

---

### 4. Payment Status Polling

#### Get Payment Intent Status
* **HTTP Method + Path**: `GET /pwa/payment-intents/{orderCode}`
* **Auth Role**: Public (No Auth Required)
* **Controller Class**: `PwaPaymentController`
* **Request DTO**: None (Path Variable `orderCode` [Long])
* **Response DTO**: `ApiResponse<PaymentIntentStatusResponse>`
* **Service Called**: `PwaPaymentService.getPaymentIntentStatus`
* **Repository / DB Tables Touched**: `payment_intents` (Read), `parking_sessions` (Read), `rfid_cards` (Read)
* **Business Behavior**: Fetches current status of a payment order. Automatically converts `PENDING` states to `EXPIRED` if the local time is past the intent's expiration deadline.

---

### 5. PayOS Webhook

#### Handle PayOS Webhook
* **HTTP Method + Path**: `POST /payments/webhooks/payos`
* **Auth Role**: Public (Validated via PayOS signature checksum)
* **Controller Class**: `PayosWebhookController`
* **Request DTO**: `Map<String, Object>` raw payload representing PayOS's payload format.
* **Response DTO**: `Map<String, Object>` (returns `{"success": true}`)
* **Service Called**: `PayosWebhookService.process`
* **Repository / DB Tables Touched**: `payment_webhook_logs` (Insert/Update), `payment_intents` (Read/Update), `parking_sessions` (Read/Update)
* **Business Behavior**: Saves received payload to logs, validates checksum, updates `PaymentIntent` status to `PAID`, updates `ParkingSession` status to `PAID`, captures checkout fields (`paid_at`, `payment_method = 'PAYOS'`, `payment_reference = orderCode`), and establishes the `exit_deadline` by adding the rule's grace minutes.

---

### 6. Staff Entry / Exit Operations

#### Staff Check-in (Creates Session)
* **HTTP Method + Path**: `POST /staff/parking-sessions/check-in`
* **Auth Role**: `ROLE_STAFF` (Bearer JWT Auth)
* **Controller Class**: `StaffParkingSessionController`
* **Request DTO**: `ParkingSessionCheckInRequest` (fields: `plateNumber`, `cardCode`, `parkingId` [optional], `vehicleTypeId` [optional], `entryImageUrl`)
* **Response DTO**: `ApiResponse<ParkingSessionCheckInResponse>`
* **Service Called**: `StaffParkingSessionService.checkIn`
* **Repository / DB Tables Touched**: `parking_sessions` (Insert), `slots` (Lock/Update status to `OCCUPIED`), `rfid_cards` (Read), `parkings` (Read), `tenants` (Read reference)
* **Business Behavior**: Begins an active session. Assigns a slot from the first available zone matching the vehicle type (if `vehicleTypeId` is provided) or picks the first available general slot. Locks slot status. Returns QR check-out token paths.

#### Staff Exit Preview
* **HTTP Method + Path**: `POST /staff/parking-sessions/exit-preview`
* **Auth Role**: `ROLE_STAFF` (Bearer JWT Auth)
* **Controller Class**: `StaffParkingSessionController`
* **Request DTO**: `ExitPreviewRequest` (fields: `cardCode`)
* **Response DTO**: `ApiResponse<ExitPreviewResponse>`
* **Service Called**: `StaffParkingSessionService.previewExit`
* **Repository / DB Tables Touched**: `rfid_cards` (Read), `parking_sessions` (Read), `pricing_rules` (Read), `slots` (Read)
* **Business Behavior**: Validates staff kiosk context (must be `EXIT` or `MIXED` type at the correct parking location). Looks up card's active session, computes pricing quote up to now, and determines exit eligibility. Emits an `ExitDecision` enum:
  * `ALLOW_EXIT`: Paid online, within grace period.
  * `COLLECT_CASH`: Unpaid online, must collect parking fee.
  * `GRACE_EXPIRED_SURCHARGE`: Paid online, but exceeded the grace period. Must collect a surcharge equal to the rule's `nextBlockPrice`.

#### Staff Complete Exit (Releases Slot)
* **HTTP Method + Path**: `POST /staff/parking-sessions/complete-exit`
* **Auth Role**: `ROLE_STAFF` (Bearer JWT Auth)
* **Controller Class**: `StaffParkingSessionController`
* **Request DTO**: `CompleteExitRequest` (fields: `sessionId`, `cardCode`, `paymentMode` [`ONLINE`, `CASH`, `SURCHARGE_CASH`], `collectedAmount`, `note` [optional])
* **Response DTO**: `ApiResponse<CompleteExitResponse>`
* **Service Called**: `StaffParkingSessionService.completeExit`
* **Repository / DB Tables Touched**: `parking_sessions` (Update), `slots` (Update status to `AVAILABLE`), `rfid_cards` (Read)
* **Business Behavior**: Completes the parking session, saves checkout timestamps, and frees the associated parking slot.
  * **Validations**: Re-evaluates exit preview logic to prevent fraudulent exit path selections. Matches requested `paymentMode` with the preview decision, enforcing `collectedAmount` thresholds.
  * **Status Transitions**: Session goes to `COMPLETED`. Session `payment_status` is updated to `CASH_COLLECTED` or `SURCHARGE_COLLECTED` depending on the path. Slot status is reset to `AVAILABLE`.

---

## PART 2 — BUSINESS FLOW EXPLANATION

```
                                  [Check-In (Staff)]
                                          │
                                          ▼
                               Assign Slot & Zone Type
                                          │
                                          ▼
                                    Create Session
                                          │
                                          ▼
┌─────────────────────────────── [Driver PWA] ──────────────────────────────┐
│                                                                           │
│   Scan QR (qrToken) ───► Fetch Active Session ───► Request Quote          │
│                                                          │                │
│                                                          ▼                │
│   Poll Status ◄─── PayOS Checkout Screen ◄─── Create Payment Intent       │
│        │                                                                  │
└────────┼──────────────────────────────────────────────────────────────────┘
         │
         ▼ (Webhook Callback)
    Verify Checksum ───► Mark Session Paid ───► Calculate Exit Deadline
                                                      │
                                                      ▼
                                            [Staff Exit Preview]
                                                      │
                                           ┌──────────┴──────────┐
                                           ▼                     ▼
                                      [Within Grace]     [Grace Expired]
                                           │                     │
                                           ▼                     ▼
                                      ALLOW_EXIT      GRACE_EXPIRED_SURCHARGE
                                           │                     │
                                           ▼                     ▼
                                     ONLINE Path        SURCHARGE_CASH Path
                                           │                     │
                                           └──────────┬──────────┘
                                                      │
                                                      ▼
                                                Release Slot
```

### 1. Manager Creates/Updates Pricing Rule
Managers define pricing plans based on target **Parking Buildings** and **Vehicle Types**.
* **Tenant Scoping & Hierarchy**: Rules can be configured as a *Parking Building Override* (binding to a specific parking building) or a *Tenant Default Rule* (where `parking_id` is null). The system attempts to match parking overrides first, and falls back to tenant default rules if none are found.
* **Active Status**: Rules are either `ACTIVE` or `INACTIVE`. Only `ACTIVE` rules are looked up during checkout calculation.
* **Duplicate Rule Checks**: The system prevents creating multiple active rules for the same scope. A database-level index `uk_pricing_rules_active_scope` enforces that only one active, non-deleted rule exists for any `(tenant_id, COALESCE(parking_id, '0000-0000...'), vehicle_type_id)` combination. Attempts to violate this restriction throw a `DUPLICATE_RESOURCE` error.

### 2. Staff Check-in
* **Work Context Resolution**: Staff accounts are bound to specific `Devices` and `Kiosks` via shift assignments. The kiosk location determines which parking building context is active.
* **RFID Card Validation**: The RFID card code is scanned. It must be in the `ACTIVE` status and not currently associated with any other `ACTIVE` parking session.
* **Slot Assignment by Zone Vehicle Type**: The system queries the `slots` database table using a pessimistic write lock (`SELECT ... FOR UPDATE`).
  * If the staff selects a specific `vehicleTypeId`, the query searches for available slots whose zone is mapped to that `vehicleTypeId` (`findFirstAvailableForCheckInByVehicleType`).
  * If no vehicle type is requested, it retrieves the first available slot and resolves the vehicle type from the slot's zone.
  * The slot is marked `OCCUPIED`, and an `ACTIVE` `ParkingSession` is saved.
* **QR Relationship**: The card is initialized with a unique, randomized `qrToken` (e.g. `qr_<hash>`). This token is embedded in the card's URL path to grant public, anonymous access to the checkout dashboard.

### 3. Driver Opens PWA
* When a driver scans the slot's QR code or the receipt code, the web PWA extracts the `qrToken` from the URL path.
* The PWA queries `/pwa/cards/{qrToken}/active-session`. The backend searches for the RFID card mapping to the token, ensures the card status is active, and retrieves the corresponding active session.
* This returns the building name, floor name, zone code, slot code, coordinate markers (`xCoordinate`, `yCoordinate`), and a user-friendly instruction guide (e.g. *"Xe cua ban o tang B1, khu B1-A, slot A-05."*).

### 4. Checkout Quote
* The driver's PWA fetches the latest payment calculation via `/pwa/cards/{qrToken}/checkout-quote`.
* **Timestamps**: The backend computes the duration from the session's check-in timestamp (`checkInAt`) up to the current time (`quotedAt`), rounding up fractions of a minute to the next integer.
* **Calculation Algorithm**:
  1. The system resolves the pricing rule. If a building override exists, it uses it; otherwise, it falls back to the default rule.
  2. Free minutes (`freeMinutes`) are subtracted from the total duration.
  3. If remaining duration is > 0, the first block price (`firstBlockPrice`) is charged for the first block interval (`firstBlockMinutes`).
  4. If further time remains, it is divided into subsequent block intervals (`nextBlockMinutes`), each costing the `nextBlockPrice`.
  5. If the calculated fee exceeds the daily cap (`dailyCapPrice`), it is capped at the daily cap.
* The response exposes a `nextAction` field to instruct the PWA (e.g., `CREATE_PAYMENT_INTENT` if unpaid, or `EXIT_WITHIN_GRACE_PERIOD` if already paid online).

### 5. Create PayOS Payment Intent
* When the driver clicks pay, the PWA creates a PayOS intent on the backend.
* **Reusability Check**: To prevent duplicate link creation on PayOS, the backend searches for an existing `PENDING`, unexpired payment intent for this session with the exact same amount. If found, it returns the existing checkout details.
* **Zero Amount Handling**: If the calculated amount is `0.00` VND (e.g., within free minutes), the backend bypasses PayOS entirely. It sets the payment intent status to `PAID`, marks the session paid, and writes a check-out deadline using the rule's grace minutes.
* **Disabled Provider Handling**: If the PayOS integration is disabled in configs, the request fails with a `400 Bad Request` mapping to `REQUEST_FAILED` code.
* **Link Generation**: If a new payment link is required, the backend fetches a unique order code, contacts the PayOS client, maps the return/cancel callback URLs, and saves the intent details.

### 6. PayOS Webhook
* **Signature Verification**: PayOS posts transactional webhooks to `/payments/webhooks/payos`. The backend extracts the payload data, recalculates the signature using HMAC-SHA256 with the configured checksum key, and matches it against the received signature. If verification fails, it logs a `FAILED` record and throws a `400` error.
* **Idempotency**: The webhook creates a log in `payment_webhook_logs`. If the transaction has already been processed (i.e. intent is `PAID`), it sets the log status to `PROCESSED` and returns early.
* **Amount Matching**: The webhook checks if the received paid amount matches the snapshotted intent amount. Mismatches throw an error and mark the log as `FAILED`.
* **State Updates**: If transaction code indicates success (`00`), the intent is marked `PAID`. The backend then updates the active parking session:
  * `payment_status` = `PAID`
  * `payment_method` = `PAYOS`
  * `payment_reference` = order code
  * `paid_at` = transaction timestamp
  * `exit_deadline` = transaction timestamp + pricing rule's `graceMinutesAfterPayment`.

### 7. Payment Polling
* While the driver is on the checkout screen, the PWA polls `/pwa/payment-intents/{orderCode}`.
* The API returns the intent status along with the resolved check-out deadline (`exitDeadline`).
* **Expiration Handling**: If the status is still `PENDING` but the local system clock has passed the intent's `expiresAt` timestamp, the backend returns a status of `EXPIRED` to trigger a checkout recalculation in the UI.

### 8. Staff Exit Preview
* When the vehicle arrives at the exit gate, the staff scans the RFID card.
* The staff device calls the exit-preview endpoint.
* **Kiosk Restrictions**: The backend ensures the staff's logged-in session is registered at a kiosk with type `EXIT` or `MIXED`, and that the kiosk's parking matches the parking session location.
* **Decision Logic**:
  * **Online Path (Grace Active)**: If session payment status is `PAID` and `now` is before `exit_deadline`, decision is `ALLOW_EXIT`. Gate opens automatically.
  * **Online Path (Grace Expired)**: If session is `PAID` but `now` is after `exit_deadline`, decision is `GRACE_EXPIRED_SURCHARGE`. The driver must pay a cash surcharge equal to the rule's `nextBlockPrice`.
  * **Cash Path**: If session is unpaid, decision is `COLLECT_CASH`. The staff must collect the total quote amount in cash.

### 9. Complete Exit
* The staff submits the final exit confirmation to `/staff/parking-sessions/complete-exit`.
* **Path Validation**: The requested payment mode must match the preview decision:
  * `ONLINE` mode is rejected unless decision is `ALLOW_EXIT`.
  * `CASH` mode is rejected unless decision is `COLLECT_CASH` and `collectedAmount` matches or exceeds the amount due.
  * `SURCHARGE_CASH` mode is rejected unless decision is `GRACE_EXPIRED_SURCHARGE` and `collectedAmount` matches or exceeds the surcharge.
* **Slot Release**:
  1. The parking session status is updated to `COMPLETED` and `checkOutAt` is captured.
  2. The session payment status is set to `CASH_COLLECTED` or `SURCHARGE_COLLECTED` if cash was handled.
  3. The associated slot's status is set back to `AVAILABLE`.
  4. The gate is authorized to open.

---

## PART 3 — DATABASE AUDIT

Here are the primary tables participating in the check-in, pricing, billing, and checkout workflows:

### 1. `pricing_rules`
Stores the pricing structures configured by managers.
* **`tenant_id`** (`UUID`, NOT NULL): Connects the rule to a specific multi-tenant scope.
* **`parking_id`** (`UUID`, NULLABLE): References the parking building. If null, this is a tenant-wide default rule.
* **`vehicle_type_id`** (`UUID`, NOT NULL): The vehicle type to which this rule applies.
* **`free_minutes`** (`INTEGER`, NOT NULL): Duration (in minutes) during which parking is free.
* **`first_block_minutes`** (`INTEGER`, NOT NULL): Duration of the initial billing interval.
* **`first_block_price`** (`NUMERIC(12,2)`, NOT NULL): Fee charged for the first block.
* **`next_block_minutes`** (`INTEGER`, NOT NULL): Duration of subsequent billing intervals.
* **`next_block_price`** (`NUMERIC(12,2)`, NOT NULL): Fee charged for each subsequent block.
* **`daily_cap_price`** (`NUMERIC(12,2)`, NULLABLE): Maximum fee charged per 24 hours.
* **`grace_minutes_after_payment`** (`INTEGER`, NOT NULL): Grace period allotted after online checkout before surcharges apply.
* **`status`** (`VARCHAR(20)`, NOT NULL): Status of the rule (`ACTIVE` or `INACTIVE`).
* **`is_deleted`** (`BOOLEAN`, NOT NULL): Soft-deletion flag.

### 2. `parking_sessions`
Represents vehicle parking stays.
* **`status`** (`VARCHAR(20)`, NOT NULL): Status of the session (`ACTIVE` or `COMPLETED`).
* **`check_in_at`** (`TIMESTAMP WITH TIME ZONE`, NOT NULL): Check-in timestamp.
* **`check_out_at`** (`TIMESTAMP WITH TIME ZONE`, NULLABLE): Check-out timestamp.
* **`parking_id`** / **`vehicle_type_id`** / **`slot_id`** / **`rfid_card_id`** (`UUID` keys): Entity references.
* **`payment_status`** (`VARCHAR(20)`, NULLABLE): Session payment state (`PAID`, `CASH_COLLECTED`, `SURCHARGE_COLLECTED`).
* **`payment_method`** (`VARCHAR(30)`, NULLABLE): Checkout method (`PAYOS`, `CASH`, `PAYOS+CASH`).
* **`payment_reference`** (`VARCHAR(100)`, NULLABLE): The PayOS `orderCode` or external cash receipt code.
* **`paid_at`** (`TIMESTAMP WITH TIME ZONE`, NULLABLE): Timestamp when payment was completed.
* **`exit_deadline`** (`TIMESTAMP WITH TIME ZONE`, NULLABLE): Calculated checkout deadline based on grace minutes.
* **`total_amount`** (`NUMERIC(12,2)`, NULLABLE): Total parking fee charged.

### 3. `payment_intents`
Tracks payment orders created via PayOS.
* **`order_code`** (`BIGINT`, NOT NULL, UNIQUE): Numeric code sent to PayOS to identify the order.
* **`status`** (`VARCHAR(20)`, NOT NULL): State of the order (`PENDING`, `PAID`, `CANCELLED`, `EXPIRED`).
* **`amount`** (`NUMERIC(12,2)`, NOT NULL): Requested billing amount.
* **`currency`** (`VARCHAR(10)`, NOT NULL): Billing currency (`VND`).
* **`provider`** (`VARCHAR(30)`, NOT NULL): Payment gateway provider (`PAYOS`).
* **`checkout_url`** / **`qr_code`** (`TEXT`, NULLABLE): PayOS payment screen links and raw QR images.
* **`expires_at`** (`TIMESTAMP WITH TIME ZONE`, NULLABLE): Expiration timestamp for the payment link.
* **`paid_at`** (`TIMESTAMP WITH TIME ZONE`, NULLABLE): Verification timestamp of payment.
* **`quote_snapshot_json`** (`TEXT`, NULLABLE): Snapshotted breakdown details.

### 4. `payment_webhook_logs`
Audit trails of received PayOS webhook posts.
* **`provider`** (`VARCHAR(30)`, NOT NULL): Gateway identifier (`PAYOS`).
* **`order_code`** (`BIGINT`, NULLABLE): Extracted transaction code.
* **`event_code`** (`VARCHAR(100)`, NULLABLE): Code received from PayOS (e.g. `'00'`).
* **`status`** (`VARCHAR(20)`, NOT NULL): Log processing state (`RECEIVED`, `PROCESSED`, `IGNORED`, `FAILED`).
* **`payload_json`** (`TEXT`, NOT NULL): Raw webhook request body.
* **`error_message`** (`TEXT`, NULLABLE): Diagnostic message if processing failed.
* **`received_at`** / **`processed_at`** (`TIMESTAMP WITH TIME ZONE`): Logging timelines.

### 5. `rfid_cards`
Physical cards mapped to parking stays.
* **`code`** (`VARCHAR(100)`): Human-readable card code (e.g., `VIN-RFID-001`).
* **`qr_token`** (`VARCHAR(120)`, UNIQUE): Token parsed by driver device to open PWA sessions.
* **`status`** (`VARCHAR(20)`): Card availability state (`ACTIVE`, `INACTIVE`).

### 6. `slots`
Allocated parking locations.
* **`status`** (`VARCHAR(20)`): State of the slot (`AVAILABLE`, `OCCUPIED`, `MAINTENANCE`, `LOCKED`).
* **`zone_id`** / **`parking_id`** (`UUID` keys): Topology references.

### 7. `zones`
Categorized zones within buildings.
* **`vehicle_type_id`** (`UUID`, NULLABLE): Filters what vehicle types can park here.

### 8. `kiosk` & `kiosk_staff`
Staff physical stations and working contexts.
* **`parking_id`** (`UUID`): References the kiosk location.
* **`type`** (`VARCHAR(20)`): Gate capability (`ENTRY`, `EXIT`, `MIXED`).
* **`is_active`** (`BOOLEAN`): Active shift assignment indicator.

---

## PART 4 — SERVICE/REPOSITORY CALL GRAPH

### A. PWA Checkout Quote
```
[PwaCardSessionController]
         │  (GET /pwa/cards/{qrToken}/checkout-quote)
         ▼
[PwaCardSessionServiceImpl]
         │  (Loads RfidCard, retrieves ACTIVE ParkingSession)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [PwaPaymentServiceImpl]
         │                                     │  (Queries reusable pending intents)
         │                                     ▼
         │                            [PaymentIntentRepository]
         ▼
[PricingQuoteServiceImpl]
         │  (Loads active PricingRule, calculates fees)
         ▼
[PricingRuleRepository]
         │  (Resolves building override or tenant default rule)
         ▼
[CardCheckoutQuoteResponse] (Returned to PWA Controller)
```

### B. PWA Create PayOS Payment Intent
```
[PwaPaymentController]
         │  (POST /pwa/cards/{qrToken}/payment-intents)
         ▼
[PwaPaymentServiceImpl]
         │  (Loads card & session; fetches checkout quote)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [PricingQuoteServiceImpl]
         │                                        │  (Calculates quote amount)
         │                                        ▼
         │                            [PaymentIntentRepository]
         │                                           (Checks if pending intent exists)
         ▼
[PayosClient (SDK)]
         │  (Calls PayOS API createPaymentLink)
         ▼
[PaymentIntentRepository]
         │  (Saves PaymentIntent entity in PENDING status)
         ▼
[PaymentIntentResponse] (Returned to PWA Controller with checkoutUrl)
```

### C. PayOS Webhook
```
[PayosWebhookController]
         │  (POST /payments/webhooks/payos)
         ▼
[PayosWebhookServiceImpl]
         │  (Persists log in RECEIVED status)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [PaymentWebhookLogRepository]
         │                                           (Saves raw JSON log)
         ▼
[PayosSignatureService]
         │  (Verifies checksum integrity)
         ▼
[PaymentIntentRepository]
         │  (Loads PaymentIntent by orderCode; checks amount; status -> PAID)
         ▼
[ParkingSessionRepository]
         │  (Sets payment_status = PAID, paid_at, exit_deadline)
         ▼
[PaymentWebhookLogRepository]
         │  (Saves log status as PROCESSED)
         ▼
[HTTP 200 OK] (Returned to PayOS)
```

### D. Staff Exit Preview
```
[StaffParkingSessionController]
         │  (POST /staff/parking-sessions/exit-preview)
         ▼
[StaffParkingSessionServiceImpl]
         │  (Checks exit/mixed kiosk work context)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [StaffWorkContextServiceImpl]
         │                                           (Resolves staff location)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [PricingQuoteServiceImpl]
         │                                           (Calculates quote amount)
         ▼
[PricingRuleRepository]
         │  (Loads rule to extract nextBlockPrice for surcharge calculation)
         ▼
[ExitPreviewResponse] (Returns Decision: ALLOW_EXIT, COLLECT_CASH, or GRACE_EXPIRED_SURCHARGE)
```

### E. Staff Complete Exit
```
[StaffParkingSessionController]
         │  (POST /staff/parking-sessions/complete-exit)
         ▼
[StaffParkingSessionServiceImpl]
         │  (Loads active session; verifies card code matches)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            [StaffWorkContextServiceImpl]
         │                                           (Verifies exit kiosk context)
         ├────────────────────────────────────────┐
         │                                        ▼
         │                            (Re-runs Exit Preview rules)
         ▼
[ParkingSessionRepository]
         │  (Session status -> COMPLETED; saves final amounts)
         ▼
[SlotRepository]
         │  (Resets status -> AVAILABLE)
         ▼
[CompleteExitResponse] (Confirms exit completed and releases gate)
```

---

## PART 5 — EXACT DEMO SCRIPT

### Preparation
> [!IMPORTANT]
> The manager control panel default parking filter may show `Vincom Thao Dien`. However, the seed staff account is bound to **Vincom Dong Khoi**.
> **You must tell Nam to switch the parking filter to "Vincom Dong Khoi" on the Manager Pricing Rule screen before configuring rules.**

* Configure `Car`, `Motorcycle`, and `Electric Car` rules under **Vincom Dong Khoi**.
* Set **`freeMinutes = 0`** to ensure a non-zero quote calculation is generated immediately for testing.
* Note the RFID cards available in the database (e.g., `'VIN-RFID-001'`).

---

### Flow 1 — Online PayOS checkout
1. **Manager Login**: Log in to the Manager web panel.
2. **Pricing Configuration**: Select **Vincom Dong Khoi**, update or create a rule for **Car** with `freeMinutes = 0`, `firstBlockMinutes = 60`, `firstBlockPrice = 20000.00`, and `graceMinutesAfterPayment = 2` (short grace period for demo purposes).
3. **Staff Login**: Log in to the Staff check-in device with `dev@vincom.smartpark.local` (local dev credentials: `Password@123`). This assigns the staff user to the **Dong Khoi Main Gate** kiosk.
4. **Staff Check-in**:
   * Plate number: `51A-12345`
   * Card code: `VIN-RFID-001`
   * Vehicle Type: `Car`
   * Click **Check In**. The slot status changes to occupied, and the card's active session is created.
5. **Open Driver PWA**: Scan the card's QR code or navigate to `/pwa/c/qr_6f99b2df94d37ad7be15e0c2f82e850b` (resolve using the QR token returned in the check-in response).
6. **Quote Verification**: The checkout quote displays the calculated fee (e.g., 20,000 VND).
7. **Initiate Payment**: Click **Pay Now**. This creates a PayOS payment intent and redirects the user to the PayOS checkout screen.
8. **Complete Payment**: Pay on the PayOS test gateway. PayOS fires a success webhook callback to the backend, marking the session paid and setting a 2-minute checkout deadline.
9. **Status Verification**: The PWA dashboard updates to show **Paid** and displays the checkout deadline.
10. **Staff Exit Preview**:
    * Scan the card at the exit gate.
    * The staff UI queries exit-preview and returns **ALLOW_EXIT**.
11. **Complete Exit**: Staff clicks **Complete Exit** (using payment mode `ONLINE`).
12. **Verify Released Slot**: The slot status resets to **AVAILABLE**.

---

### Flow 2 — Cash fallback path
1. **Staff Check-in**:
   * Check in a vehicle with card `VIN-RFID-001` and plate number `51A-99999`.
2. **Driver Quote**: Open the PWA link to view the outstanding quote balance. Do not pay online.
3. **Staff Exit Preview**:
   * Vehicle arrives at the exit gate. Staff scans the card.
   * The exit-preview endpoint returns **COLLECT_CASH** with the total calculated amount due (e.g., 20,000 VND).
4. **Complete Exit**:
   * The driver pays cash to the staff.
   * Staff completes the exit by selecting payment mode **`CASH`** and submitting the collected amount.
5. **Verify Session Details**: Verify the session status is `COMPLETED`, payment status is `CASH_COLLECTED`, and the slot is **`AVAILABLE`**.

---

### Flow 3 — Grace expired surcharge path
1. **Prepare Session**: Complete steps 1-8 of Flow 1 to check in a vehicle and pay online.
2. **Simulate Deadline Exceeded**:
   * Wait past the 2-minute grace period *or* manually update the session's check-out deadline to a past timestamp:
     ```sql
     UPDATE parking_sessions
     SET exit_deadline = CURRENT_TIMESTAMP - INTERVAL '5 minutes'
     WHERE license_plate = '51A-12345' AND status = 'ACTIVE';
     ```
3. **Staff Exit Preview**:
   * Scan the card at the exit gate.
   * The exit-preview endpoint returns **GRACE_EXPIRED_SURCHARGE**, requesting a cash surcharge equal to the rule's `nextBlockPrice` (e.g., 10,000 VND).
4. **Complete Exit**:
   * Staff collects the cash surcharge from the driver.
   * Staff completes the exit by selecting payment mode **`SURCHARGE_CASH`** and submitting the collected surcharge.
5. **Verify Session Details**: Verify the session status is `COMPLETED`, payment status is `SURCHARGE_COLLECTED`, payment method is `PAYOS+CASH`, and the slot is **`AVAILABLE`**.

---

## PART 6 — SQL CHECKLIST

Copy-pasteable PostgreSQL queries for verification during the demo:

### 1. Active Pricing Rules for Vincom Dong Khoi
```sql
SELECT id, name, free_minutes, first_block_minutes, first_block_price, next_block_minutes, next_block_price, grace_minutes_after_payment, status
FROM pricing_rules
WHERE parking_id = md5('parking-vincom-dongkhoi')::uuid
  AND status = 'ACTIVE'
  AND is_deleted = false;
```

### 2. Available Vehicle Types
```sql
SELECT id, name, code, is_active
FROM vehicle_types
WHERE is_deleted = false;
```

### 3. Active Staff Session & Kiosk Location Context
```sql
SELECT u.id AS user_id, u.username, k.id AS kiosk_id, k.name AS kiosk_name, k.type AS kiosk_type, p.name AS parking_name
FROM users u
JOIN kiosk_staff ks ON ks.staff_user_id = u.id AND ks.is_active = true
JOIN kiosk k ON k.id = ks.kiosk_id AND k.is_deleted = false
JOIN parkings p ON p.id = k.parking_id AND p.is_deleted = false
WHERE u.username = 'staff@vincom.smartpark.local' OR u.username = 'dev@vincom.smartpark.local';
```

### 4. Get Available (Unassigned) RFID Cards
```sql
SELECT id, code, uid, qr_token, status
FROM rfid_cards
WHERE tenant_id = md5('tenant-vincom')::uuid
  AND status = 'ACTIVE'
  AND id NOT IN (
      SELECT rfid_card_id
      FROM parking_sessions
      WHERE status = 'ACTIVE'
        AND rfid_card_id IS NOT NULL
  );
```

### 5. View Active Parking Sessions
```sql
SELECT id, license_plate, check_in_at, status, payment_status, total_amount, exit_deadline, slot_id, rfid_card_id
FROM parking_sessions
WHERE tenant_id = md5('tenant-vincom')::uuid
  AND status = 'ACTIVE';
```

### 6. Verify Vehicle Type Matches Allocated Slot Zone Type
```sql
SELECT ps.id AS session_id, ps.license_plate, vt.code AS session_vehicle_type,
       s.code AS slot_code, z.code AS zone_code, z_vt.code AS zone_vehicle_type
FROM parking_sessions ps
JOIN vehicle_types vt ON vt.id = ps.vehicle_type_id
JOIN slots s ON s.id = ps.slot_id
JOIN zones z ON z.id = s.zone_id
LEFT JOIN vehicle_types z_vt ON z_vt.id = z.vehicle_type_id
WHERE ps.status = 'ACTIVE';
```

### 7. View Live Payment Intent Log (for Polling/Webhook checks)
```sql
SELECT id, order_code, amount, status, checkout_url, expires_at, paid_at, parking_session_id
FROM payment_intents
ORDER BY created_at DESC
LIMIT 5;
```

### 8. View Webhook logs
```sql
SELECT id, order_code, event_code, status, error_message, received_at, processed_at
FROM payment_webhook_logs
ORDER BY received_at DESC
LIMIT 5;
```

### 9. View Completed Sessions & Slot Release status
```sql
SELECT ps.id AS session_id, ps.license_plate, ps.check_out_at, ps.status AS session_status,
       ps.payment_status, ps.payment_method, ps.total_amount, s.code AS slot_code, s.status AS slot_status
FROM parking_sessions ps
JOIN slots s ON s.id = ps.slot_id
WHERE ps.tenant_id = md5('tenant-vincom')::uuid
ORDER BY ps.check_out_at DESC
LIMIT 5;
```

---

## PART 7 — RISK CHECK & MITIGATION

### 1. PayOS Provider Disabled or Unconfigured
* **Symptom**: Clicking **Pay Now** returns a `400 Bad Request` with message *"REQUEST_FAILED"* in the response payload.
* **Likely Cause**: `payos.enabled` is set to false, or PayOS credentials (`PAYOS_API_KEY`, `PAYOS_CLIENT_ID`, etc.) are missing from the configuration.
* **Quick Fix**: Check the `.env` environment variables or log in to the server shell to set `PAYOS_ENABLED=true` and provide valid test credentials. Alternatively, proceed using the **Cash Fallback path** (Flow 2) during the demo.

### 2. PayOS Webhook Delays or Fails to Fire
* **Symptom**: The PWA checkout screen remains stuck on pending/loading, and the payment intent does not transition to `PAID` status.
* **Likely Cause**: The webhook listener is inaccessible from the public internet (CORS / Firewall issues), or signature key validation failed.
* **Quick Fix**: Inspect `payment_webhook_logs` to check for received events. If webhooks are blocked, staff can use the **Cash Fallback path** at the exit gate, or run manual database overrides to set the payment state:
  ```sql
  UPDATE parking_sessions
  SET payment_status = 'PAID', paid_at = CURRENT_TIMESTAMP, exit_deadline = CURRENT_TIMESTAMP + INTERVAL '15 minutes'
  WHERE license_plate = '51A-12345' AND status = 'ACTIVE';
  ```

### 3. Duplicate Active Pricing Rules Exist
* **Symptom**: Fetching checkout quotes throws a `500` or `400` error with message `"MULTIPLE_PRICING_RULES_MATCH"`.
* **Likely Cause**: Conflicting active pricing rules were manually added to the database, bypassing the Java validation checks.
* **Quick Fix**: Run SQL to set conflicting rules to `INACTIVE`:
  ```sql
  UPDATE pricing_rules
  SET status = 'INACTIVE'
  WHERE id <> '<keep-id>' AND parking_id = md5('parking-vincom-dongkhoi')::uuid;
  ```

### 4. No Available Slot for Checked-in Vehicle Type
* **Symptom**: Staff check-in fails with error *"No available slot for selected vehicle type"*.
* **Likely Cause**: All slots configured under zones for the vehicle type in Vincom Dong Khoi are marked `OCCUPIED`, `MAINTENANCE`, or `LOCKED`.
* **Quick Fix**: Clean up active sessions to release slots, or change a slot status back to available:
  ```sql
  UPDATE slots
  SET status = 'AVAILABLE'
  WHERE zone_id = md5('zone:vincom-dk:b1-a')::uuid;
  ```

### 5. Duplicate Overview Sidebar Displays on Deployed Frontend
* **Symptom**: Two navigation sidebars appear overlapping on the staff dashboard interface.
* **Likely Cause**: A layout file imports the Sidebar component twice, or routing layouts are nested incorrectly on the frontend.
* **Quick Fix**: This is a known UI issue on the frontend. Refreshing the browser or resizing the viewport resolves the double sidebar rendering.

### 6. Card QR Code Scan Shows "CARD_NOT_ACTIVE" or "NO_ACTIVE_SESSION"
* **Symptom**: Scanning the card QR code displays an error screen.
* **Likely Cause**: The scanned RFID card is `INACTIVE` in the database, or the vehicle check-in step was skipped/failed.
* **Quick Fix**: Verify that the card's status is `ACTIVE` and that there is an active check-in session for the card:
  ```sql
  UPDATE rfid_cards SET status = 'ACTIVE' WHERE code = 'VIN-RFID-001';
  ```
