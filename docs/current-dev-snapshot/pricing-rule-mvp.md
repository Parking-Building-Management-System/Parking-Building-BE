# Pricing Rule MVP

## Scope

Flow 2A adds Manager-configured hourly pricing rules and a quote calculation service. It does not
create invoices, payment intents, webhooks, paid sessions, exit passes, or Staff Exit completion.

## Database

Table: `pricing_rules`

Key fields:

- `tenant_id`: manager tenant scope.
- `parking_id`: nullable. `null` is tenant default; non-null is parking-specific.
- `vehicle_type_id`: required global vehicle type.
- `free_minutes`
- `first_block_minutes`
- `first_block_price`
- `next_block_minutes`
- `next_block_price`
- `daily_cap_price`
- `grace_minutes_after_payment`
- `status`: `ACTIVE` or `INACTIVE`
- `is_deleted`: soft delete flag

Active uniqueness:

- One active, non-deleted rule per tenant + parking scope + vehicle type.
- `parking_id = null` is treated as its own tenant-default scope.

## Rule Selection

1. Active rule for tenant + exact parking + vehicle type.
2. Active tenant default rule for tenant + `parking_id is null` + vehicle type.
3. If neither exists, quote fails with `PRICING_RULE_NOT_CONFIGURED`.
4. If more than one active rule matches the same scope, quote fails with
   `MULTIPLE_PRICING_RULES_MATCH`.

## Algorithm

1. `durationMinutes = ceil(secondsBetween(checkInAt, quoteAt) / 60)`.
2. `chargeableMinutes = max(0, durationMinutes - freeMinutes)`.
3. If chargeable minutes are zero, amount is `0`.
4. First block charges `firstBlockPrice` for up to `firstBlockMinutes`.
5. Remaining minutes charge `ceil(remaining / nextBlockMinutes) * nextBlockPrice`.
6. If `dailyCapPrice` is configured, final amount is capped to it.
7. Currency is fixed to `VND`.

## Manager API

All endpoints require `ROLE_PARKING_MANAGER`. Tenant comes from JWT.

```http
GET /manager/pricing/rules?parkingId=&vehicleTypeId=&status=&page=0&size=20
POST /manager/pricing/rules
GET /manager/pricing/rules/{id}
PUT /manager/pricing/rules/{id}
PATCH /manager/pricing/rules/{id}/status
DELETE /manager/pricing/rules/{id}
POST /manager/pricing/rules/{id}/preview
```

Create/update request:

```json
{
  "name": "Car standard Bcons",
  "parkingId": "parking-id-or-null",
  "vehicleTypeId": "vehicle-type-id",
  "freeMinutes": 10,
  "firstBlockMinutes": 120,
  "firstBlockPrice": 20000,
  "nextBlockMinutes": 60,
  "nextBlockPrice": 10000,
  "dailyCapPrice": 100000,
  "graceMinutesAfterPayment": 15,
  "status": "ACTIVE"
}
```

Status request:

```json
{
  "status": "INACTIVE"
}
```

Preview request:

```json
{
  "checkInAt": "2026-05-28T10:00:00Z",
  "checkOutAt": "2026-05-28T12:35:00Z",
  "vehicleTypeId": "vehicle-type-id"
}
```

Preview response result:

```json
{
  "pricingRuleId": "rule-id",
  "pricingRuleName": "Car standard Bcons",
  "checkInAt": "2026-05-28T17:00:00",
  "quotedAt": "2026-05-28T19:35:00",
  "durationMinutes": 155,
  "chargeableMinutes": 145,
  "amount": 30000,
  "currency": "VND",
  "pricingBreakdown": [
    {
      "label": "First block",
      "minutes": 120,
      "unitPrice": 20000,
      "amount": 20000
    },
    {
      "label": "Additional block x 1",
      "minutes": 25,
      "unitPrice": 10000,
      "amount": 10000
    }
  ]
}
```

## Demo Seed

Migration `V20260531110000__add_pricing_rules.sql` seeds deterministic parking-specific rules for
the existing Vincom, FPT, and Bcons demo parkings and vehicle types:

- `CAR`: first 120 minutes 20,000 VND, next 60 minutes 10,000 VND, daily cap 100,000 VND.
- `ELECTRIC_CAR`: first 120 minutes 25,000 VND, next 60 minutes 12,000 VND, daily cap 120,000 VND.
- `MOTORBIKE`: first 240 minutes 5,000 VND, next 120 minutes 5,000 VND, daily cap 30,000 VND.

Seed inserts only when no active non-deleted rule exists for the same tenant, parking, and vehicle
type.

## Limitations

- No zone/floor-specific pricing yet.
- No day/night, holiday, overnight, subscription, or penalty pricing yet.
- No invoice/payment mutation.
- Daily cap is a simple quote cap for the current MVP calculation.
- Timestamps follow the backend's existing `LocalDateTime` persistence model.

## Future Phases

- Phase 2B: payment intent, mock VietQR/webhook, mark paid, set exit deadline.
- Phase 2C: Staff Exit Gate, complete checkout, release slot, surcharge after expired grace.
