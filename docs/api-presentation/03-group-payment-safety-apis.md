# Group 3 - Pricing, Payment & Safety Compliance APIs

## 1. Group Purpose

This group explains money flow and safety compliance. It shows how SmartPark calculates parking fees, creates PayOS payment intents, processes webhooks, enforces exit grace periods, and tracks fire extinguisher inspections.

The key message for the teacher: payment and safety APIs update important state and keep auditability, not just display data.

## 2. Main Actors

- `PARKING_MANAGER`: configures pricing and fire extinguisher inventory, reviews maps/logs.
- Driver/PWA: views quote, creates payment intent, polls payment status.
- PayOS: sends webhook callbacks.
- `STAFF`: performs fire inspections and uses exit gate logic.

## 3. Endpoint Table

| Method | Path | Actor/Role | Purpose | Main Request DTO | Main Response DTO | DB tables touched |
|---|---|---|---|---|---|---|
| GET | `/manager/pricing/rules` | `PARKING_MANAGER` | List pricing rules | filters, page, size | `PageResponse<ManagerPricingRuleResponse>` | `pricing_rules`, `parkings`, `vehicle_types` |
| POST | `/manager/pricing/rules` | `PARKING_MANAGER` | Create pricing rule | `ManagerPricingRuleRequest` | `ManagerPricingRuleResponse` | `pricing_rules`, `parkings`, `vehicle_types` |
| GET | `/manager/pricing/rules/{id}` | `PARKING_MANAGER` | Get rule detail | path id | `ManagerPricingRuleResponse` | `pricing_rules` |
| PUT | `/manager/pricing/rules/{id}` | `PARKING_MANAGER` | Update rule values | `ManagerPricingRuleRequest` | `ManagerPricingRuleResponse` | `pricing_rules` |
| PATCH | `/manager/pricing/rules/{id}/status` | `PARKING_MANAGER` | Activate/inactivate rule | `ManagerPricingRuleStatusRequest` | `ManagerPricingRuleResponse` | `pricing_rules` |
| DELETE | `/manager/pricing/rules/{id}` | `PARKING_MANAGER` | Soft-delete rule | path id | `ApiResponse<Void>` | `pricing_rules` |
| POST | `/manager/pricing/rules/{id}/preview` | `PARKING_MANAGER` | Preview a quote without writing session/payment | `ManagerPricingRulePreviewRequest` | `PricingQuoteResponse` | `pricing_rules` read only |
| GET | `/pwa/cards/{qrToken}/checkout-quote` | Driver/PWA | Calculate current fee and next action | QR token | `CardCheckoutQuoteResponse` | `rfid_cards`, `parking_sessions`, `pricing_rules` |
| POST | `/pwa/cards/{qrToken}/payment-intents` | Driver/PWA | Create/reuse PayOS intent | QR token | `PaymentIntentResponse` | `payment_intents`, `parking_sessions`, `pricing_rules`, `rfid_cards` |
| GET | `/pwa/payment-intents/{orderCode}` | Driver/PWA | Poll payment status and exit deadline | orderCode | `PaymentIntentStatusResponse` | `payment_intents`, `parking_sessions` |
| POST | `/payments/webhooks/payos` | PayOS | Verify webhook and mark payment/session paid | webhook JSON | `{ "success": true }` | `payment_webhook_logs`, `payment_intents`, `parking_sessions` |
| GET | `/manager/fire-extinguishers` | `PARKING_MANAGER` | List fire extinguishers | filters, page, size | `PageResponse<FireExtinguisherResponse>` | `fire_extinguishers` |
| POST | `/manager/fire-extinguishers` | `PARKING_MANAGER` | Create extinguisher | `FireExtinguisherRequest` | `FireExtinguisherResponse` | `fire_extinguishers`, `parkings`, `floors`, `zones` |
| GET | `/manager/fire-extinguishers/{id}` | `PARKING_MANAGER` | Get extinguisher | path id | `FireExtinguisherResponse` | `fire_extinguishers` |
| PUT | `/manager/fire-extinguishers/{id}` | `PARKING_MANAGER` | Update extinguisher | `FireExtinguisherRequest` | `FireExtinguisherResponse` | `fire_extinguishers` |
| PATCH | `/manager/fire-extinguishers/{id}/status` | `PARKING_MANAGER` | Update status | `FireExtinguisherStatusRequest` | `FireExtinguisherResponse` | `fire_extinguishers` |
| PATCH | `/manager/fire-extinguishers/{id}/coordinate` | `PARKING_MANAGER` | Update map pin | `FireExtinguisherCoordinateRequest` | `FireExtinguisherResponse` | `fire_extinguishers` |
| DELETE | `/manager/fire-extinguishers/{id}` | `PARKING_MANAGER` | Soft-delete extinguisher | path id | `ApiResponse<Void>` | `fire_extinguishers` |
| GET | `/manager/fire-extinguishers/summary` | `PARKING_MANAGER` | Counts by status/due/expired | none | `FireExtinguisherSummaryResponse` | `fire_extinguishers` |
| GET | `/manager/floors/{floorId}/fire-safety-map` | `PARKING_MANAGER` | Floor map with extinguisher pins | path floorId | `FireSafetyMapResponse` | `floors`, `fire_extinguishers` |
| GET | `/manager/fire-inspections/logs` | `PARKING_MANAGER` | Review inspection logs | filters, page, size | `PageResponse<FireInspectionLogResponse>` | `fire_extinguisher_inspections`, `fire_extinguishers` |
| GET | `/staff/fire-inspections/due` | `STAFF` | List inspections due in kiosk parking | floorId, status | `List<StaffFireInspectionDueResponse>` | `fire_extinguishers`, `kiosk` |
| POST | `/staff/fire-inspections` | `STAFF` | Submit checklist and optional photo key | `StaffFireInspectionRequest` | `StaffFireInspectionResponse` | `fire_extinguisher_inspections`, `fire_extinguishers` |
| POST | `/staff/fire-inspections/photos/presign-upload` | `STAFF` | Create upload URL for inspection photo | `StaffInspectionPhotoPresignRequest` | `StaffInspectionPhotoPresignResponse` | storage only, later referenced by inspection |

## 4. Typical Demo Sequence

1. Manager creates or reviews a pricing rule.
2. Manager runs pricing preview with sample check-in/check-out times.
3. Driver PWA gets checkout quote by QR token.
4. Driver PWA creates a payment intent.
5. Demo operator shows payment status polling.
6. Explain real PayOS webhook to `/payments/webhooks/payos`.
7. Staff exit gate uses paid status and exit deadline.
8. Manager opens fire extinguisher list/summary/map.
9. Staff gets due inspections, presigns photo upload, submits inspection.
10. Manager reviews inspection logs with photo display URL if object key is present.

## 5. Important Validations and Business Rules

- Only one active pricing rule should exist for the same tenant, parking scope, and vehicle type.
- Pricing preview is read-only and validates checkout time is after check-in time.
- Checkout quote requires active card, active session, active pricing rule, and a vehicle type.
- Payment intent creation reuses an unexpired pending intent when amount is unchanged.
- Zero-amount quotes are marked paid immediately without PayOS checkout.
- PayOS webhook must pass checksum verification before state changes.
- Webhook amount must match the stored payment intent amount.
- Paid webhook updates both `payment_intents.status` and `parking_sessions.payment_status`.
- Exit deadline is calculated from `paidAt + graceMinutesAfterPayment`.
- Fire extinguisher code is unique per tenant.
- Extinguisher floor/zone must belong to the selected parking.
- Coordinates must be valid percentage pins.
- Staff can inspect only extinguishers in the trusted kiosk parking.
- Inspection submission writes a log and updates extinguisher last/next inspection state.
- Photo upload returns an object key; the backend never stores raw image bytes in the inspection request.

## 6. Common Error Codes

- `PRICING_RULE_NOT_CONFIGURED`: no active pricing rule for the session.
- `PAYMENT_PROVIDER_DISABLED` or `REQUEST_FAILED`: PayOS not configured or provider call failed.
- `PAYOS_INVALID_SIGNATURE`: webhook checksum failed.
- `PAYOS_AMOUNT_MISMATCH`: paid amount does not match intent.
- `NO_ACTIVE_SESSION_FOR_CARD`: QR/card has no active parking session.
- `FORBIDDEN_ACTION`: wrong role, kiosk context missing, or extinguisher outside kiosk parking.
- `RESOURCE_NOT_FOUND`: pricing rule, payment intent, extinguisher, floor, parking, or session not found.
- `DUPLICATE_RESOURCE`: duplicate pricing rule or extinguisher code.
- `INVALID_INPUT`: invalid coordinates, dates, content type, or checklist payload.

## 7. How to Test With Bruno

Use folders:

- `06 Manager Pricing`
- `05 PWA Driver`
- `07 PWA Payment PayOS`
- `08 Staff Exit Gate`
- `10 Fire Safety`

Important variables:

- `managerToken`, `staffEntryToken`, `staffExitToken`
- `parkingId`, `floorId`, `zoneId`, `vehicleTypeId`
- `pricingRuleId`
- `cardCode`, `qrToken`, `sessionId`
- `paymentOrderCode`
- `fireExtinguisherId`
- `inspectionPhotoObjectKey`

Run order:

1. Create/list pricing rule.
2. Run pricing preview.
3. Use a checked-in session QR token for PWA quote.
4. Create payment intent and copy `paymentOrderCode`.
5. Poll status. Use real PayOS only when credentials/webhook are configured.
6. Fire Safety: create extinguisher, map, due inspections, presign photo, submit inspection, review logs.

## 8. Presentation Notes

- Money flow should be explained as: quote -> intent -> PayOS -> webhook -> paid session -> exit deadline.
- Tell the audience that webhook verification is the trust boundary for provider callbacks.
- Fire Safety should be framed as compliance: inventory, map pins, due list, inspection evidence, manager review.
- Do not show real PayOS keys, MinIO keys, webhook signatures, or production QR data.
