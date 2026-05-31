# Staff Exit Gate MVP

## Scope

Flow 2C adds STAFF exit-gate APIs for previewing an exit decision and completing an active parking
session. It reuses Flow 2A pricing and Flow 2B payment state. It does not change PayOS payment
intent/webhook behavior, create invoices, implement subscriptions, or add a full debt module.

## Lifecycle

`parking_sessions.status`:

- `ACTIVE`: vehicle is still inside the parking.
- `COMPLETED`: staff completed exit and the vehicle left.

On completion:

- `check_out_at` is set to server current time.
- `status` becomes `COMPLETED`.
- `total_amount` is set to the online paid amount, cash amount due, or online paid amount plus
  surcharge.
- Assigned slot status becomes `AVAILABLE`.
- RFID card remains `ACTIVE`; reuse is enforced by the existing "no ACTIVE session for this card"
  rule.
- Existing PayOS `payment_reference` is kept for online payments.

## Kiosk Validation

The authenticated STAFF user's approved device resolves a work context from the current session,
device, kiosk, and kiosk staff assignment.

Exit APIs require kiosk type `EXIT` or `MIXED`. `ENTRY` kiosks are rejected with
`EXIT_KIOSK_REQUIRED`. The active session must belong to the kiosk parking, otherwise the API returns
`SESSION_NOT_IN_KIOSK_PARKING`.

## Exit Preview

```http
POST /staff/parking-sessions/exit-preview
Authorization: Bearer <staff-access-token>
Content-Type: application/json
```

Request:

```json
{
  "cardCode": "BCONS-0004"
}
```

Behavior:

1. Resolve staff kiosk context.
2. Require `EXIT` or `MIXED` kiosk.
3. Resolve RFID card by tenant and card code.
4. Find latest `ACTIVE` parking session for that card.
5. Require session parking to match kiosk parking.
6. Recalculate current quote with existing pricing rules.
7. Return a decision without mutating the session.

Paid online within grace:

```json
{
  "sessionId": "uuid",
  "plateNumber": "30A-99125",
  "cardCode": "BCONS-0004",
  "parkingName": "Bcons Plaza",
  "floorName": "Basement 1",
  "zoneName": "B1 Resident Cars",
  "slotCode": "C-04",
  "checkInAt": "2026-05-31T08:00:00",
  "paidAt": "2026-05-31T10:30:00",
  "exitDeadline": "2026-05-31T10:45:00",
  "paymentStatus": "PAID",
  "exitDecision": "ALLOW_EXIT",
  "amountDue": 0,
  "surchargeAmount": 0,
  "totalAmount": 30000,
  "currency": "VND",
  "message": "Paid online. Allow exit."
}
```

Paid online after grace:

```json
{
  "exitDecision": "GRACE_EXPIRED_SURCHARGE",
  "amountDue": 0,
  "surchargeAmount": 10000,
  "message": "Grace period expired. Collect surcharge."
}
```

Unpaid:

```json
{
  "exitDecision": "COLLECT_CASH",
  "amountDue": 30000,
  "surchargeAmount": 0,
  "message": "Cash payment required."
}
```

## Surcharge MVP

If a PayOS-paid session is past `exitDeadline`, surcharge is the matched pricing rule's
`nextBlockPrice`. The preview response includes `totalAmount = onlinePaid + surchargeAmount`.

## Complete Exit

```http
POST /staff/parking-sessions/complete-exit
Authorization: Bearer <staff-access-token>
Content-Type: application/json
```

Request:

```json
{
  "sessionId": "uuid",
  "cardCode": "BCONS-0004",
  "paymentMode": "ONLINE",
  "collectedAmount": 0,
  "note": "optional"
}
```

Allowed `paymentMode`:

- `ONLINE`: requires preview decision `ALLOW_EXIT`; `collectedAmount` must be `0`.
- `CASH`: requires `COLLECT_CASH`; `collectedAmount >= amountDue`.
- `SURCHARGE_CASH`: requires `GRACE_EXPIRED_SURCHARGE`; `collectedAmount >= surchargeAmount`.

The service recalculates the preview server-side before completion to avoid stale UI decisions.

Response:

```json
{
  "sessionId": "uuid",
  "status": "COMPLETED",
  "plateNumber": "30A-99125",
  "cardCode": "BCONS-0004",
  "checkInAt": "2026-05-31T08:00:00",
  "checkOutAt": "2026-05-31T10:40:00",
  "paymentMode": "ONLINE",
  "collectedAmount": 0,
  "totalAmount": 30000,
  "currency": "VND",
  "slotCode": "C-04",
  "slotStatus": "AVAILABLE",
  "cardStatus": "ACTIVE",
  "message": "Exit completed. Gate can open."
}
```

## Edge Cases

- Unknown card: `RFID_CARD_NOT_FOUND`
- No active session: `NO_ACTIVE_SESSION_FOR_CARD`
- Wrong kiosk parking: `SESSION_NOT_IN_KIOSK_PARKING`
- Entry-only kiosk: `EXIT_KIOSK_REQUIRED`
- Already completed session: `SESSION_ALREADY_COMPLETED`
- Card code does not match session: `CARD_CODE_DOES_NOT_MATCH_SESSION`
- Payment mode does not match recalculated decision: `ONLINE_EXIT_NOT_ALLOWED`,
  `CASH_EXIT_NOT_ALLOWED`, or `SURCHARGE_EXIT_NOT_ALLOWED`
- Insufficient cash: `CASH_AMOUNT_TOO_LOW` or `SURCHARGE_AMOUNT_TOO_LOW`

## Frontend Notes

- Use preview first after scanning/typing a card code.
- Do not rely on preview for final authorization; completion recalculates.
- For `ALLOW_EXIT`, send `paymentMode=ONLINE` and `collectedAmount=0`.
- For `COLLECT_CASH`, collect at least `amountDue`, then send `paymentMode=CASH`.
- For `GRACE_EXPIRED_SURCHARGE`, collect at least `surchargeAmount`, then send
  `paymentMode=SURCHARGE_CASH`.

## Limitations

- No invoice or receipt generation.
- No cash ledger.
- No audit log in this MVP.
- No gate/barrier hardware command; response message indicates the FE can open the gate in demo.
- No change tracking for overpayment/change due.
