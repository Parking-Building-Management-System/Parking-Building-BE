# PWA Checkout Quote MVP

## Scope

Flow 2A exposed a public quote endpoint for a driver holding an RFID card QR. Flow 2B keeps the
endpoint read-only and enhances it with PayOS payment state so the PWA can decide whether to create
a new payment intent, continue an existing pending payment, or show the paid grace-period state.

## Endpoint

```http
GET /pwa/cards/{qrToken}/checkout-quote
```

Authentication: none. The endpoint follows the existing possession-based public card QR model used
by `GET /pwa/cards/{qrToken}/active-session`.

## Behavior

1. Resolve `rfid_cards.qr_token`.
2. Require card status `ACTIVE`.
3. Find latest `ACTIVE` parking session for the card.
4. Resolve pricing rule by tenant + parking + vehicle type.
5. Calculate quote at server current time.
6. Resolve payment state from existing payment intent/session payment fields.
7. Return session, location, price, breakdown data, and next payment action.
8. Do not mutate the parking session.

## Response Shape

```json
{
  "sessionId": "session-id",
  "plateNumber": "30A-99125",
  "licensePlate": "30A-99125",
  "cardCode": "BCONS-0004",
  "status": "ACTIVE",
  "checkInAt": "2026-05-28T10:00:00",
  "quotedAt": "2026-05-28T12:35:00",
  "durationMinutes": 155,
  "chargeableMinutes": 145,
  "vehicleTypeId": "vehicle-type-id",
  "vehicleTypeName": "Car",
  "parkingName": "Bcons Plaza Residential Parking",
  "floorName": "Basement 1",
  "zoneName": "B1 Resident Cars",
  "slotCode": "C-04",
  "amount": 30000,
  "currency": "VND",
  "pricingRuleId": "pricing-rule-id",
  "pricingRuleName": "Car standard Bcons",
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
  ],
  "paymentAvailable": true,
  "paymentStatus": null,
  "paidAt": null,
  "exitDeadline": null,
  "existingPaymentIntent": {
    "orderCode": 202605310001,
    "status": "PENDING",
    "checkoutUrl": "https://pay.payos.vn/...",
    "expiresAt": "2026-05-28T12:50:00"
  },
  "nextAction": "CONTINUE_PAYMENT"
}
```

The response is wrapped in the standard `ApiResponse`.

## Payment Actions

- `CREATE_PAYMENT_INTENT`: PayOS is enabled/configured and no reusable pending intent exists.
- `CONTINUE_PAYMENT`: a same-session, same-amount, non-expired pending intent exists.
- `EXIT_WITHIN_GRACE_PERIOD`: session `payment_status=PAID`; PWA should show `paidAt` and
  `exitDeadline`.
- `PAYMENT_PROVIDER_DISABLED`: PayOS is disabled or not configured.

## Error Cases

- Unknown QR token: `CARD_QR_NOT_FOUND`
- Card inactive: `CARD_NOT_ACTIVE`
- Card has no active session: `NO_ACTIVE_SESSION_FOR_CARD`
- No active matching pricing rule: `PRICING_RULE_NOT_CONFIGURED`
- Multiple active matching rules: `MULTIPLE_PRICING_RULES_MATCH`

## Frontend Notes

- Use this endpoint separately from active-session guide data.
- Refresh quote manually when the user taps refresh; no polling is required for MVP.
- Show `Pay & Exit` when `paymentAvailable=true` and `nextAction=CREATE_PAYMENT_INTENT`.
- Reopen the stored PayOS checkout URL when `nextAction=CONTINUE_PAYMENT`.
- Show paid/grace state when `nextAction=EXIT_WITHIN_GRACE_PERIOD`.
- Show staff assistance or retry messaging when `paymentAvailable=false`.
- For no pricing rule, show staff assistance / pricing not configured.

## Limitations

- No invoice generation.
- No surcharge.
- No Staff Exit Gate completion.

## Future Phases

- Phase 2C adds Staff Exit Gate completion, slot release, and surcharge handling.
