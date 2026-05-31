# SmartPark Bruno Collection

This collection covers manual API checks for the current SmartPark backend flows:

- System Admin tenant, master data, permissions, health, audit, sessions, and devices
- Manager facility, floor, zone, slot, map, storage, RFID, staff, kiosk, device, and pricing APIs
- Staff entry check-in
- Driver PWA active-session and checkout quote APIs
- PayOS payment intent and webhook endpoints
- Staff exit preview and session completion

## Setup

1. Install Bruno from https://www.usebruno.com.
2. Open the folder `bruno/SmartPark` as a collection.
3. Select the `Local` environment for local testing or copy `Production.example` for production-safe placeholders.
4. Start the backend locally on port `8081` when using `Local`.
5. Login and paste returned access tokens into the matching environment variables.

Never store real JWTs, refresh tokens, PayOS keys, or webhook signatures in the collection.

## Environment Variables

- `baseUrl`: API base URL, for example `http://localhost:8081`.
- `systemAdminToken`: SYSTEM_ADMIN access token.
- `managerToken`: PARKING_MANAGER access token.
- `staffEntryToken`: STAFF token from an approved ENTRY or ENTRY_EXIT kiosk device.
- `staffExitToken`: STAFF token from an approved EXIT or ENTRY_EXIT kiosk device.
- `tenantId`, `parkingId`, `floorId`, `zoneId`, `slotId`: facility identifiers copied from manager/admin responses.
- `staffId`, `kioskId`, `exitKioskId`, `deviceApprovalId`, `deviceId`: staff/device setup identifiers.
- `cardCode`, `rfidCardId`, `qrToken`, `sessionId`: parking session identifiers.
- `pricingRuleId`, `vehicleTypeId`: pricing identifiers.
- `paymentOrderCode`: PayOS numeric order code returned by payment intent creation.

## Flow 1: Entry And PWA Map

1. Login manager and configure parking, floor, zone, slot, RFID cards, staff, and kiosk.
2. Approve the staff device against an ENTRY or ENTRY_EXIT kiosk.
3. Login staff with the approved device fingerprint and set `staffEntryToken`.
4. Run `04 Staff Entry / Check In Parking Session`.
5. Copy `sessionId` and `qrToken` from the response.
6. Run `05 PWA Driver / Get Active Session By QR Token`.

Expected PWA response includes parking/floor/zone/slot names, `mapDisplayUrl`, and slot coordinates.

## Flow 2A: Checkout Quote

1. Create or activate a pricing rule for the parking and vehicle type.
2. Run `05 PWA Driver / Get Checkout Quote`.
3. Verify amount, duration, breakdown, payment status, and `nextAction`.

Common failures: `PRICING_RULE_NOT_CONFIGURED`, missing active session, or inactive RFID card.

## Flow 2B: PayOS Payment

1. Run `07 PWA Payment PayOS / Create Payment Intent`.
2. Copy `orderCode` into `paymentOrderCode`.
3. Open the returned `checkoutUrl` and finish payment through PayOS.
4. Poll `07 PWA Payment PayOS / Get Payment Intent Status`.
5. Real PayOS webhooks are sent to `/payments/webhooks/payos`.

The included webhook request is sample-only and intentionally uses an invalid signature. Expected behavior is rejection when signature verification fails. Do not place PayOS client IDs, API keys, checksum keys, or real webhook signatures in Bruno.

## Flow 2C: Staff Exit Gate

1. Approve an exit staff device against an EXIT or ENTRY_EXIT kiosk.
2. Login staff and set `staffExitToken`.
3. Run `08 Staff Exit Gate / Exit Preview`.
4. If `exitDecision` is `ALLOW_EXIT`, run Complete Online Exit.
5. If `exitDecision` is `COLLECT_CASH`, run Complete Cash Exit with at least `amountDue`.
6. If `exitDecision` is `GRACE_EXPIRED_SURCHARGE`, run Complete Surcharge Exit with at least `surchargeAmount`.

Expected completion response has status `COMPLETED`, slot status `AVAILABLE`, and card status `ACTIVE`. ENTRY-only kiosks should be rejected for exit.

## Common Errors

- `DEVICE_NOT_TRUST`: staff login/device is not approved by a manager.
- `KIOSK_CONTEXT_REQUIRED`: staff request lacks a trusted kiosk context.
- `NO_ACTIVE_SESSION_FOR_CARD`: no ACTIVE parking session exists for the scanned card.
- `PRICING_RULE_NOT_CONFIGURED`: no active pricing rule matches the session.
- `PAYMENT_PROVIDER_DISABLED`: PayOS is disabled or unavailable in the current environment.
- Invalid PayOS webhook signature: sample webhook or real webhook checksum does not match.
- ENTRY kiosk rejected for exit: use an EXIT or ENTRY_EXIT kiosk for staff exit gate tests.

## DB Validation SQL

Active sessions:

```sql
select id, plate_number, status, payment_status, check_in_at, check_out_at
from parking_sessions
where status = 'ACTIVE'
order by check_in_at desc;
```

Paid sessions:

```sql
select id, plate_number, payment_status, paid_at, exit_deadline, payment_reference
from parking_sessions
where payment_status = 'PAID'
order by paid_at desc;
```

Slot and card release after exit:

```sql
select ps.id, ps.status, ps.check_out_at, s.code as slot_code, s.status as slot_status, c.code as card_code, c.status as card_status
from parking_sessions ps
join parking_slots s on s.id = ps.slot_id
join rfid_cards c on c.id = ps.rfid_card_id
where ps.id = '<session-id>';
```

Payment intents:

```sql
select order_code, status, amount, paid_at, parking_session_id
from payment_intents
order by created_at desc
limit 20;
```

Webhook logs:

```sql
select provider, order_code, status, received_at, processed_at, error_message
from payment_webhook_logs
order by received_at desc
limit 20;
```
