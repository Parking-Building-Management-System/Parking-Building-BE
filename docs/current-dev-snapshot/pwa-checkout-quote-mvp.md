# PWA Checkout Quote MVP

## Scope

Flow 2A exposes a public quote endpoint for a driver holding an RFID card QR. The endpoint computes
the current parking fee for the active session and returns a disabled payment state. It does not
create invoices, payment intents, webhooks, paid status, exit deadlines, or checkout completion.

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
6. Return session, location, price, and breakdown data.
7. Do not mutate the parking session.

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
  "paymentAvailable": false,
  "nextAction": "PAYMENT_PENDING_IMPLEMENTATION"
}
```

The response is wrapped in the standard `ApiResponse`.

## Error Cases

- Unknown QR token: `CARD_QR_NOT_FOUND`
- Card inactive: `CARD_NOT_ACTIVE`
- Card has no active session: `NO_ACTIVE_SESSION_FOR_CARD`
- No active matching pricing rule: `PRICING_RULE_NOT_CONFIGURED`
- Multiple active matching rules: `MULTIPLE_PRICING_RULES_MATCH`

## Frontend Notes

- Use this endpoint separately from active-session guide data.
- Refresh quote manually when the user taps refresh; no polling is required for MVP.
- Show `Pay & Exit` disabled while `paymentAvailable` is `false`.
- For no pricing rule, show staff assistance / pricing not configured.

## Limitations

- No payment gateway or VietQR.
- No webhook.
- No invoice generation.
- No paid state or exit deadline.
- No surcharge.
- No Staff Exit Gate completion.

## Future Phases

- Phase 2B adds payment intent/mock payment and session paid state.
- Phase 2C adds Staff Exit Gate completion, slot release, and surcharge handling.
