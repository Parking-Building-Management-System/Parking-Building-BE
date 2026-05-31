# Pricing Billing Backend Audit

Audit date: 2026-05-31

Scope: Manager Pricing & Billing readiness and PWA Checkout Quote design for Flow 2A. This is an
audit/design snapshot only. No pricing, billing, payment, webhook, or exit implementation was added.

## Files Inspected

- `src/main/java/com/smartpark/swp391/modules/operation/entity/ParkingSession.java`
- `src/main/java/com/smartpark/swp391/modules/operation/repository/ParkingSessionRepository.java`
- `src/main/java/com/smartpark/swp391/modules/staff/service/impl/StaffParkingSessionServiceImpl.java`
- `src/main/java/com/smartpark/swp391/modules/pwa/controller/PwaCardSessionController.java`
- `src/main/java/com/smartpark/swp391/modules/pwa/service/impl/PwaCardSessionServiceImpl.java`
- `src/main/java/com/smartpark/swp391/modules/pwa/dto/CardActiveSessionResponse.java`
- `src/main/java/com/smartpark/swp391/modules/vehicle/entity/VehicleType.java`
- `src/main/java/com/smartpark/swp391/modules/vehicle/entity/UserVehicleLink.java`
- `src/main/java/com/smartpark/swp391/modules/billing/entity/Invoice.java`
- `src/main/java/com/smartpark/swp391/modules/billing/entity/WebhookLog.java`
- `src/main/java/com/smartpark/swp391/modules/subscription/entity/Subscription.java`
- `src/main/resources/db/migration/V20260520100000__init_core_domain_tables.sql`
- `src/main/resources/db/migration/V20260520110500__seed_core_facility_data.sql`
- `src/main/resources/db/migration/V20260518093000__seed_identity_auth.sql`
- `docs/current-dev-snapshot/pwa-card-active-session-mvp.md`
- `docs/current-dev-snapshot/parking-session-checkin-mvp.md`
- `docs/backend-spec-reconciliation/*.md`

## Current Readiness Summary

Flow 1 is ready enough to feed Flow 2A: staff check-in creates an `ACTIVE` `parking_sessions` row
with tenant, parking, zone, slot, RFID card, vehicle type, license plate, and `check_in_at`. The
public PWA can resolve an active session by card QR and show guide/map data.

Pricing is not ready. There are no pricing entities, migrations, repositories, services,
controllers, manager APIs, or calculation service.

Billing is partially present as passive data model only. `invoice`, `webhook_log`, and
`subscriptions` tables/entities exist, with seed rows, but there are no repositories, services,
controllers, payment transaction table, payment state machine, invoice generation flow, or webhook
handler.

Flow 2A Checkout Quote should start with a new pricing rule table plus a quote-only PWA endpoint.
It should not generate invoices or mutate sessions yet.

## Current Pricing Schema Readiness

| Question | Current state |
|---|---|
| Pricing/time-rule tables exist? | No. Permission seed references `PricingPolicy`, but no table/entity exists. |
| Pricing matrix tables exist? | No. |
| Rules tenant-scoped? | Not applicable yet. They must be tenant-scoped when added. |
| Rules parking-scoped or tenant-global? | Not implemented. Recommended: `parking_id` nullable, where null means tenant default and parking-specific overrides default. |
| Rules vehicle-type-specific? | Not implemented. Recommended: required `vehicle_type_id` referencing global `vehicle_types`. |
| Rules zone/floor-specific? | Not implemented. Recommended: defer; add optional join table later if needed. |
| Monthly/subscription rules present? | No rule table. `subscriptions.monthly_price` exists but no setup API/job. |
| Grace period config exists? | No. Recommended: `grace_minutes_after_payment` on pricing rule. |

## Parking Session Billing Readiness

Current `parking_sessions` fields:

| Field | Exists? | Notes |
|---|---:|---|
| `check_in_at` | Yes | Required. Set by staff check-in using server `LocalDateTime.now()`. |
| `check_out_at` | Yes | Nullable. No checkout service writes it yet. |
| `total_amount` | Yes | Nullable. Seeded completed session has value. No calculation writes it yet. |
| `vehicle_type_id` | Yes | Required. Check-in uses request `vehicleTypeId` or selected zone vehicle type. |
| `status` | Yes | Enum: `ACTIVE`, `COMPLETED`, `VIOLATION`, `CANCELLED`. |
| `paid_at` | No | Missing from session. Invoice has `paid_at`. |
| `payment_status` | No | Missing. |
| `payment_method` | No | Missing. |
| `payment_reference` | No | Missing. |
| `exit_deadline` | No | Missing. Needed after payment grace period. |
| `surcharge_amount` | No | Missing. Needed after expired exit grace. |
| `invoice_id` | No | Missing on session. Invoice has FK to parking session instead. |
| `subscription_id` | No | Missing. Current subscription relation is indirect via user/vehicle/parking only. |

Readiness conclusion: good enough for quote input, not enough for payment/exit lifecycle. For Flow
2A, read `check_in_at`, `vehicle_type_id`, location/card data, and compute a transient quote. For
Flow 2B/2C, add payment/exit fields or model them in invoice/payment/exit-pass tables.

## Invoice Payment Readiness

Current tables/entities:

- `invoice`: tenant-scoped invoice table mapped by Java entity `Invoice`.
- `webhook_log`: tenant-scoped webhook log table mapped by Java entity `WebhookLog`.
- `subscriptions`: tenant-scoped subscription table mapped by Java entity `Subscription`.
- `rfid_cards`: tenant-scoped cards with unique public `qr_token`.
- `user_vehicle_link`: tenant-scoped driver/user vehicle link.

| Question | Current state |
|---|---|
| Are invoices generated? | No. Seed rows exist, but no invoice generation service/API. |
| Can one parking session have one invoice? | Schema permits multiple invoices per session because `invoice.parking_session_id` is not unique. Add a partial/unique constraint later if the business rule is one active session invoice. |
| Can surcharge create additional invoice/payment? | Schema can store another `PENALTY` invoice for same session, but no service/state model exists. |
| Is webhook idempotency supported? | No unique constraint on `(provider, external_id)` or similar. `webhook_log.external_id` is nullable and not unique. |
| Is payment reference unique? | No payment table and no payment reference field. |
| Are payment logs stored? | Only generic `webhook_log` seed rows. No `payment_transactions` or payment ledger exists. |

Recommended payment model later:

- `payment_transactions`: tenant id, invoice id, parking session id nullable, provider, method,
  amount, currency, status, external_reference, paid_at, raw_payload, created_at, updated_at.
- Unique constraint on `(tenant_id, provider, external_reference)` when reference is not null.
- Keep `webhook_log` for raw inbound events and idempotency status.

## Recommended MVP Pricing Model

Use one simple table first:

```sql
pricing_rules (
  id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  parking_id UUID NULL REFERENCES parkings(id),
  vehicle_type_id UUID NOT NULL REFERENCES vehicle_types(id),
  name VARCHAR(120) NOT NULL,
  free_minutes INTEGER NOT NULL DEFAULT 0,
  first_block_minutes INTEGER NOT NULL,
  first_block_price NUMERIC(12,2) NOT NULL,
  next_block_minutes INTEGER NOT NULL,
  next_block_price NUMERIC(12,2) NOT NULL,
  daily_cap_price NUMERIC(12,2) NULL,
  grace_minutes_after_payment INTEGER NOT NULL DEFAULT 15,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  is_deleted BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
)
```

Recommended indexes/constraints:

```sql
CREATE INDEX idx_pricing_rules_tenant_parking_vehicle
  ON pricing_rules (tenant_id, parking_id, vehicle_type_id, status, is_deleted);

CREATE UNIQUE INDEX uk_pricing_rules_active_scope
  ON pricing_rules (tenant_id, COALESCE(parking_id, '00000000-0000-0000-0000-000000000000'::uuid), vehicle_type_id)
  WHERE status = 'ACTIVE' AND is_deleted = false;
```

Do not add `pricing_rule_zones` for MVP. Zone/floor-specific pricing can be added later with a join
table:

```sql
pricing_rule_zones (pricing_rule_id UUID, zone_id UUID)
```

## Pricing Algorithm Proposal

Inputs: `checkInAt`, `quotedAt`, `vehicleTypeId`, `parkingId`, tenant id.

Rule selection:

1. Find active, non-deleted rule for `(tenant_id, parking_id, vehicle_type_id)`.
2. If none, find active tenant default rule `(tenant_id, parking_id IS NULL, vehicle_type_id)`.
3. If none, return `NO_PRICING_RULE_CONFIGURED`.
4. If more than one active match is possible, reject with `MULTIPLE_PRICING_RULES_MATCH`.

Calculation:

1. `durationMinutes = ceil(secondsBetween(checkInAt, quotedAt) / 60)`, minimum 0.
2. If `durationMinutes <= free_minutes`, amount is 0.
3. Remaining minutes after free minutes are billed.
4. First block: charge `first_block_price` for up to `first_block_minutes`.
5. Next blocks: `ceil(remaining / next_block_minutes) * next_block_price`.
6. If `daily_cap_price` is configured, cap each 24-hour period to that price.
7. Overnight has no special rule in MVP; long stays are handled by duration plus daily cap.
8. `grace_minutes_after_payment` is returned for future payment/exit logic but not applied to quote.
9. Currency is fixed as `VND`.

Example breakdown:

```json
[
  {"label":"Free minutes","minutes":15,"unitPrice":0,"amount":0},
  {"label":"First 60 minutes","minutes":60,"unitPrice":20000,"amount":20000},
  {"label":"Next 2 blocks x 30 minutes","minutes":60,"unitPrice":10000,"amount":20000}
]
```

## Flow 2A PWA Checkout Quote Endpoint Design

Endpoint:

```http
GET /pwa/cards/{qrToken}/checkout-quote
```

Authentication: none, same possession-based public QR model as active-session guide.

Behavior:

- Resolve `rfid_cards.qr_token`.
- Require card status `ACTIVE`.
- Find latest `ACTIVE` parking session for the card.
- Require a pricing rule match.
- Compute transient quote at server time.
- Do not create invoice.
- Do not update parking session.
- Do not create payment transaction.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "sessionId": "019e62c7-3e00-7b0b-9d7e-3bb58f9b4b6a",
    "plateNumber": "51A-12345",
    "licensePlate": "51A-12345",
    "cardCode": "VIN-0001",
    "status": "ACTIVE",
    "checkInAt": "2026-05-31T08:00:00",
    "quotedAt": "2026-05-31T10:17:00",
    "durationMinutes": 137,
    "vehicleTypeId": "2b1bbf30-65cd-327b-a4a5-4d49ee80b2a6",
    "vehicleTypeName": "Car",
    "parkingName": "Vincom Dong Khoi",
    "floorName": "B1",
    "zoneName": "B1 Zone A",
    "slotCode": "A-05",
    "amount": 40000,
    "currency": "VND",
    "pricingRuleId": "33333333-3333-3333-3333-333333333333",
    "pricingRuleName": "Car hourly default",
    "pricingBreakdown": [
      {"label": "First 60 minutes", "minutes": 60, "unitPrice": 20000, "amount": 20000},
      {"label": "Next 3 blocks x 30 minutes", "minutes": 77, "unitPrice": 10000, "amount": 30000},
      {"label": "Daily cap", "minutes": 137, "unitPrice": 40000, "amount": -10000}
    ],
    "paymentAvailable": false,
    "nextAction": "PAYMENT_PENDING_IMPLEMENTATION"
  },
  "timestamp": "2026-05-31T03:17:00Z",
  "path": "/pwa/cards/opaque-token/checkout-quote"
}
```

Recommended DTOs:

- `CheckoutQuoteResponse`
- `PricingBreakdownItemResponse`
- `PricingQuoteService`
- `PricingRuleRepository`

## Manager Pricing Setup API Proposal

Base scope: `ROLE_PARKING_MANAGER`, tenant from JWT, never from request body.

### List Rules

```http
GET /manager/pricing/rules?parkingId=&vehicleTypeId=&status=&page=0&size=20
```

Response: paged `PricingRuleResponse`.

### Create Rule

```http
POST /manager/pricing/rules
Content-Type: application/json
```

```json
{
  "parkingId": null,
  "vehicleTypeId": "2b1bbf30-65cd-327b-a4a5-4d49ee80b2a6",
  "name": "Car hourly default",
  "freeMinutes": 15,
  "firstBlockMinutes": 60,
  "firstBlockPrice": 20000,
  "nextBlockMinutes": 30,
  "nextBlockPrice": 10000,
  "dailyCapPrice": 150000,
  "graceMinutesAfterPayment": 15,
  "status": "ACTIVE"
}
```

Validation:

- `parkingId`, if present, must belong to manager tenant.
- `vehicleTypeId` must exist and be active.
- minute fields must be positive except `freeMinutes`, which may be zero.
- prices must be zero or positive.
- only one active rule per `(tenant, parking nullable scope, vehicleType)`.

### Detail / Update / Status / Delete

```http
GET /manager/pricing/rules/{id}
PUT /manager/pricing/rules/{id}
PATCH /manager/pricing/rules/{id}/status
DELETE /manager/pricing/rules/{id}
```

Status request:

```json
{"status":"INACTIVE"}
```

Delete should soft-delete. Status enum should start with `ACTIVE`, `INACTIVE`.

### Preview

```http
POST /manager/pricing/rules/{id}/preview
```

```json
{
  "checkInAt": "2026-05-31T08:00:00",
  "checkOutAt": "2026-05-31T10:17:00",
  "vehicleTypeId": "2b1bbf30-65cd-327b-a4a5-4d49ee80b2a6"
}
```

Response: same amount and breakdown shape as PWA quote, with rule id/name.

## Subscription And Invoice Scope

Subscriptions: audit only for now. Existing `subscriptions` table has user, vehicle, parking, period,
date range, monthly price, status, and auto-renew. Missing repository/service/controller/job. Do not
couple subscriptions into Flow 2A unless the owner explicitly wants monthly pass deduction/exemption.

Invoices: audit only for now. Existing `invoice` table can represent subscription and parking
session invoices, but no generation or payment lifecycle exists. Do not generate invoices in Flow
2A quote; generate them later when payment intent/mock payment is introduced.

Debts: no debt table or manager debt API. Debt can initially be derived from unpaid/overdue invoices
after invoice APIs exist.

## Edge Cases

- No active session for QR card: return `404 NO_ACTIVE_SESSION_FOR_CARD`.
- Unknown QR token: return `404 CARD_QR_NOT_FOUND`.
- Card inactive/suspended: return `403 CARD_NOT_ACTIVE`.
- No pricing rule configured: return `400/404 NO_PRICING_RULE_CONFIGURED`; FE should show manager
  setup required, not a zero fee.
- Multiple active pricing rules match: return `409 MULTIPLE_PRICING_RULES_MATCH`.
- Vehicle type missing from session: currently impossible by schema, but still guard with
  `INVALID_SESSION_VEHICLE_TYPE`.
- Session already `PAID`: no session payment status exists yet. Once added, quote should return
  paid state and exit deadline instead of new amount.
- Session already `COMPLETED`: return `SESSION_ALREADY_COMPLETED`.
- Session `CANCELLED` or `VIOLATION`: quote should not proceed without a product decision.
- Clock/timezone: current code uses `LocalDateTime`; MVP should compute with server clock only.
  Later, standardize timestamps to `Instant` or `OffsetDateTime` for payment/webhook correctness.
- Long duration: use daily cap if configured; otherwise blocks can grow without cap.
- Grace after payment: return config in rule/quote later, but do not apply until payment is
  implemented.
- Surcharge later: if driver pays then exits after `exit_deadline`, compute surcharge as a separate
  quote/payment/invoice item in Phase 2C.

## Implementation Phases

### Phase 2A-1 Backend

- Add `pricing_rules` migration/entity/repository/service.
- Add Manager CRUD APIs for pricing rules.
- Add `PricingQuoteService`.
- Add `GET /pwa/cards/{qrToken}/checkout-quote`.
- Add deterministic demo seed pricing rules for existing tenants/vehicle types.
- Add unit tests for rule selection and price calculation.

### Phase 2A-2 Frontend

- Manager Pricing setup UI.
- PWA active-session page displays check-in time, duration, current fee, and disabled/coming-soon
  `Pay & Exit` CTA using quote endpoint.

### Phase 2B Payment Mock

- Add payment intent/mock VietQR endpoint.
- Add `payment_transactions`.
- Add webhook simulation/idempotency.
- Mark invoice/session paid.
- Set `exit_deadline = paidAt + graceMinutesAfterPayment`.

### Phase 2C Staff Exit Gate

- Staff exit lookup by card/plate/session.
- Complete paid exit.
- Release slot.
- Handle expired grace and surcharge.

## DB Changes Needed

For Flow 2A-1:

- New `pricing_rules` table.
- Optional `PricingRuleStatus` enum in Java.
- Seed rules for demo tenants.

For Flow 2B/2C:

- Add payment table, for example `payment_transactions`.
- Add idempotency unique index to webhook/payment reference.
- Add session payment/exit fields or equivalent exit-pass table:
  - `payment_status`
  - `paid_at`
  - `payment_method`
  - `payment_reference`
  - `exit_deadline`
  - `surcharge_amount`
  - optional `subscription_id`
- Consider unique active invoice constraint for one active parking-session invoice:
  `(tenant_id, parking_session_id, invoice_type)` where status is not `VOID`.

## What Should Be Implemented First

Implement Phase 2A-1 first: `pricing_rules` + Manager pricing CRUD + shared quote calculation +
PWA checkout quote endpoint. This unlocks the Flow 2A demo without committing to payment provider,
webhook, invoice, or exit-gate state machine decisions.

## Atomic Commit Suggestions For Future Implementation

- `feat(pricing): add tenant scoped pricing rule schema`
- `feat(manager-pricing): add pricing rule crud APIs`
- `feat(pricing): add parking quote calculation service`
- `feat(pwa-checkout): add card checkout quote endpoint`
- `test(pricing): cover rule selection and block pricing`
- `docs(pricing): document manager pricing and pwa quote contracts`
