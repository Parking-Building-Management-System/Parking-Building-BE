# PayOS Payment MVP

## Scope

Flow 2B adds PayOS payment links for PWA checkout. It creates a `PAYOS` payment intent for the
active RFID card parking session, processes verified PayOS webhooks, marks payment state as paid,
and sets a grace-period exit deadline. Staff Exit Gate completion, slot release, surcharge, invoices,
subscriptions, and cash payments are still out of scope.

## Environment

```env
PAYOS_ENABLED=true
PAYOS_CLIENT_ID=...
PAYOS_API_KEY=...
PAYOS_CHECKSUM_KEY=...
PAYOS_WEBHOOK_URL=https://smartpark-api.dorriss.com/payments/webhooks/payos
PAYOS_RETURN_URL=https://parking-building-fe-ui.vercel.app/pwa/payment/success
PAYOS_CANCEL_URL=https://parking-building-fe-ui.vercel.app/pwa/payment/cancel
```

Mapped properties:

- `app.payos.enabled`
- `app.payos.client-id`
- `app.payos.api-key`
- `app.payos.checksum-key`
- `app.payos.webhook-url`
- `app.payos.return-url`
- `app.payos.cancel-url`

The application starts when PayOS variables are missing if `PAYOS_ENABLED=false`. Payment creation
returns `PAYMENT_PROVIDER_DISABLED` or `PAYOS_NOT_CONFIGURED` instead of exposing secrets or failing
unrelated flows.

## Database

Migration: `V20260531120000__add_payos_payment_intents.sql`.

New table: `payment_intents`

- `order_code BIGINT UNIQUE` is the PayOS numeric order code.
- `status`: `PENDING`, `PAID`, `CANCELLED`, `EXPIRED`, `FAILED`.
- Stores amount/currency, checkout URL, QR code, provider link id, pricing rule id, quote snapshot,
  provider raw response, paid/cancel/expires timestamps, and soft-delete flag.

New table: `payment_webhook_logs`

- Stores raw PayOS webhook payloads, signature, order code, event code, processing status, and error.
- This table is not tenant-scoped so invalid or unknown-order webhooks can be logged safely.

Added to `parking_sessions`:

- `payment_status`
- `payment_method`
- `payment_reference`
- `paid_at`
- `exit_deadline`

`parking_sessions.status` remains `ACTIVE` after online payment. Flow 2C Staff Exit Gate completes
the session only after staff confirms exit.

## Endpoints

### Create Payment Intent

```http
POST /pwa/cards/{qrToken}/payment-intents
```

Authentication: none.

Behavior:

1. Resolve active RFID card by QR token.
2. Find latest active parking session.
3. Calculate quote with existing pricing rules.
4. Reuse an existing non-expired `PENDING` intent for the same session and amount.
5. Create a unique numeric order code.
6. For amount `<= 0`, mark the intent and session `PAID` immediately without calling PayOS.
7. For positive amount, create a PayOS payment link and store checkout metadata.

Example response:

```json
{
  "paymentIntentId": "uuid",
  "orderCode": 202605310001,
  "amount": 30000,
  "currency": "VND",
  "status": "PENDING",
  "provider": "PAYOS",
  "checkoutUrl": "https://pay.payos.vn/...",
  "qrCode": "...",
  "expiresAt": "2026-05-31T10:15:00",
  "description": "SPK202605310001",
  "paidAt": null,
  "exitDeadline": null
}
```

### Payment Status

```http
GET /pwa/payment-intents/{orderCode}
```

Authentication: none.

Example response:

```json
{
  "orderCode": 202605310001,
  "status": "PAID",
  "amount": 30000,
  "currency": "VND",
  "paidAt": "2026-05-31T10:30:00",
  "exitDeadline": "2026-05-31T10:45:00",
  "sessionId": "uuid",
  "plateNumber": "51A-12345",
  "cardCode": "BCONS-0004",
  "checkoutUrl": "https://pay.payos.vn/...",
  "qrCode": "..."
}
```

### PayOS Webhook

```http
POST /payments/webhooks/payos
```

Authentication: none.

The handler stores the raw payload first, verifies PayOS HMAC checksum from `data` and `signature`,
then processes the event. Invalid signatures return `400` and are logged as `FAILED`.

Successful paid webhook behavior:

- Validate `orderCode` exists.
- Validate paid amount equals the payment intent amount.
- If already paid, return success without changing state.
- Mark `payment_intents.status=PAID`.
- Set `payment_intents.paid_at`.
- Set `parking_sessions.payment_status=PAID`.
- Set `parking_sessions.payment_method=PAYOS`.
- Set `parking_sessions.payment_reference={orderCode}`.
- Set `parking_sessions.paid_at`.
- Set `parking_sessions.exit_deadline = paidAt + pricingRule.graceMinutesAfterPayment`.

Response:

```json
{ "success": true }
```

## Idempotency

- Same active session, same amount, non-expired pending intent: return the existing PayOS link.
- Changed amount, expired, cancelled, failed, or paid intent: create or report the current state as
  appropriate.
- Duplicate paid webhook: no double update; returns success.
- Unknown valid order code: webhook log is marked `IGNORED`.
- Amount mismatch: webhook log is marked `FAILED`; payment is not marked paid.

## Local Testing

With PayOS disabled:

```bash
PAYOS_ENABLED=false ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Expected:

- App starts.
- Checkout quote returns `paymentAvailable=false` and `nextAction=PAYMENT_PROVIDER_DISABLED`.
- Positive-amount payment intent creation returns `PAYMENT_PROVIDER_DISABLED`.

With PayOS enabled, set all required PayOS variables and use:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then call:

```http
POST http://localhost:8081/pwa/cards/{qrToken}/payment-intents
```

Expected: response contains `checkoutUrl`, `orderCode`, and no secrets in logs or response.

## PayOS Dashboard

Configure webhook URL:

```text
https://smartpark-api.dorriss.com/payments/webhooks/payos
```

Use the same checksum key as `PAYOS_CHECKSUM_KEY`. The backend verifies the webhook signature and
does not trust payloads that fail checksum verification.

## Limitations

- Staff Exit Gate completion is handled by Flow 2C, not by PayOS webhook processing.
- No surcharge after grace period.
- No invoice generation.
- No subscriptions.
- No reconciliation job or payment status polling from PayOS.
- Expired pending intents are treated as expired in read responses; no background expiry job yet.
