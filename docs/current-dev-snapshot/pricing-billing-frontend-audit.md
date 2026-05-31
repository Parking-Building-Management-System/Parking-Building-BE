# Pricing & Billing Frontend Audit

Date: 2026-05-31

## Scope

Audited Manager Pricing & Billing routes, public PWA card route, and Staff Exit readiness for the next Flow 2A implementation slice. No backend integration or payment gateway work was implemented.

## Files Inspected

- `src/app/(protected)/manager/pricing/page.tsx`
- `src/app/(protected)/manager/pricing/time-rules/page.tsx`
- `src/app/(protected)/manager/pricing/matrix/page.tsx`
- `src/app/(protected)/manager/pricing/subscriptions/page.tsx`
- `src/app/(protected)/manager/pricing/invoices/page.tsx`
- `src/app/(protected)/manager/pricing/debts/page.tsx`
- `src/app/(protected)/staff/exit/page.tsx`
- `src/app/pwa/c/[qrToken]/page.tsx`
- `src/features/pwa/card-active-session-guide.tsx`
- `src/service/pwa/api.ts`
- `src/service/pwa/type.ts`
- `src/service/staff/api.ts`
- `src/service/staff/type.ts`
- `src/config/mock-pages.ts`
- `src/config/navigation.ts`
- `src/components/mock-module-page.tsx`

## Route Coverage

| Route | Exists | Current implementation | Readiness |
| --- | --- | --- | --- |
| `/manager/pricing` | Yes | `MockModulePage` | Navigation and placeholder layout exist; needs real overview component and pricing summary API. |
| `/manager/pricing/time-rules` | Yes | `MockModulePage` | Placeholder lists planned rule and preview needs; needs full CRUD UI. |
| `/manager/pricing/matrix` | Yes | `MockModulePage` | Placeholder exists; can become a compact matrix/table view of pricing rules. |
| `/manager/pricing/subscriptions` | Yes | `MockModulePage` | Placeholder exists; out of Flow 2A unless subscriptions are started later. |
| `/manager/pricing/invoices` | Yes | `MockModulePage` | Placeholder exists; out of Flow 2A until billing records are implemented. |
| `/manager/pricing/debts` | Yes | `MockModulePage` | Placeholder exists; out of Flow 2A until debt/reminder APIs exist. |
| `/pwa/c/[qrToken]` | Yes | Real active-session guide | Public active-session API is wired; checkout quote UI/API is missing. |
| `/staff/exit` | Yes | `MockModulePage` | Staff exit cashier route exists but is mock/API pending. |

## Current Pricing Route Findings

- All Manager Pricing & Billing pages currently render the shared `MockModulePage` with `Mock page / API pending`.
- The manager sidebar already includes Pricing & Billing child routes and active-route behavior through the existing navigation system.
- The mock pages have useful basic structure: module title, planned scope bullets, and planned backend API list.
- No dedicated pricing service, types, React Query keys, forms, tables, or mutation flows exist yet.
- No pricing route currently loads real data or performs mutations.
- Current mock planned API names in `mock-pages.ts` use older `time-rules` / `matrix` wording; Flow 2A should align on `/manager/pricing/rules`.

## Buttons And Actions Currently API Pending

- Pricing overview: all future setup navigation/summary actions.
- Time Rules: list, create, edit, status, delete, and preview calculator.
- Pricing Matrix: rule table display and any edit/publish actions.
- Subscriptions: list, create, renew, cancel, reminders.
- Invoices: list, status tracking, PDF/download.
- Debts: list, reminders, adjustment notes.
- Staff Exit: search active sessions, quote amount, collect/confirm payment, complete checkout.
- PWA Checkout: quote refresh and Pay & Exit are not present yet.

## Manager Pricing Setup UI Proposal

### `/manager/pricing/time-rules`

Use a production-style CRUD page similar to Facility and Staff pages:

- Header: `Time Rules`, short explanation, `Create Rule` button.
- Filters: parking, vehicle type, status, search by rule name.
- Table columns:
  - rule name
  - parking
  - vehicle type
  - free minutes
  - first block
  - next block
  - daily cap
  - grace after payment
  - status
  - updated time
  - actions
- Row actions:
  - edit
  - activate/deactivate through status patch
  - delete with confirmation if backend supports delete
  - preview calculation
- Create/edit modal fields:
  - `name`
  - `parkingId`
  - `vehicleTypeCode`
  - `freeMinutes`
  - `firstBlockMinutes`
  - `firstBlockPrice`
  - `nextBlockMinutes`
  - `nextBlockPrice`
  - `dailyCapPrice`
  - `graceMinutesAfterPayment`
  - `status`
- Validation:
  - required name, parking, vehicle type, status
  - minute fields are integers >= 0
  - price fields are integers or decimals >= 0 based on backend money convention
  - next block requires both minutes and price
  - daily cap can be blank/null, otherwise >= first block price

### Preview Calculator

Embed a side panel or modal on Time Rules:

- Inputs:
  - `checkInAt`
  - `checkOutAt`
  - `vehicleTypeCode`
  - optional `parkingId` when not already scoped
- Output:
  - calculated amount
  - chargeable duration
  - free minutes applied
  - first block charge
  - next block count/charge
  - daily cap applied flag
  - grace-after-payment note
  - rule version/name used
- Call `POST /manager/pricing/rules/{id}/preview`.
- Keep preview separate from saved rule mutations.

### `/manager/pricing/matrix`

Keep this intentionally compact:

- Filters: parking and status.
- Table grouped by parking, with vehicle types as rows.
- Columns: vehicle type, active rule, free minutes, first block price, next block price, daily cap, status.
- Actions: view/edit rule, open preview.
- Do not build a complex spreadsheet editor in Flow 2A; reuse the Time Rules create/edit modal or route to Time Rules.

## PWA Checkout Quote UI Proposal

Extend `CardActiveSessionGuide` after the current active-session details and map:

- Session summary:
  - plate number
  - card code
  - parking/floor/zone/slot
  - check-in time
  - current duration
- Checkout quote panel:
  - current quote amount
  - pricing breakdown rows
  - quote timestamp
  - refresh quote button
  - `Pay & Exit` CTA disabled with `API pending` or `Flow 2A pending` until payment/checkout is started
- Quote states:
  - `ACTIVE_UNPAID`: show amount, breakdown, refresh, disabled Pay & Exit CTA.
  - `PAID`: show paid state and exit guidance later.
  - `COMPLETED`: show completed/closed state later.
  - `NO_ACTIVE_SESSION`: show current no-session state.
  - `NO_PRICING_RULE`: show clear message that pricing is not configured and staff assistance is required.

Current PWA route notes:

- `GET /pwa/cards/{qrToken}/active-session` is already wired through a public axios client.
- Current PWA labels and error text are Vietnamese; Flow 2A should switch visible labels to English for consistency with the current frontend direction.
- Query key is primitive/stable and no polling is used.
- Checkout quote should use a separate stable query key, likely `['pwa-checkout-quote', qrToken]`, with manual refresh and no polling.

## Staff Exit Readiness

`/staff/exit` exists and renders `MockModulePage` for `Exit Cashier`.

Current placeholder scope:

- search active sessions by plate or card code
- show entry image, current image placeholder, and bill amount
- complete session and release slot after payment confirmation

Fields Staff Exit will need later:

- search input: plate number, card code, or session id
- session fields: session id, plate number, card code, parking, floor, zone, slot, check-in time, duration, status
- media fields: entry image URL, exit/current image URL
- quote fields: amount, currency, pricing rule name/version, breakdown lines, quote timestamp, no-pricing-rule reason
- payment fields: payment method, paid amount, payment status, paid at, receipt/reference id
- checkout fields: check-out time, staff user id/name, released slot status, completion status

## API Contracts Needed

### Manager Pricing Rules

```http
GET /manager/pricing/rules
POST /manager/pricing/rules
GET /manager/pricing/rules/{id}
PUT /manager/pricing/rules/{id}
PATCH /manager/pricing/rules/{id}/status
DELETE /manager/pricing/rules/{id}
POST /manager/pricing/rules/{id}/preview
```

Suggested rule list params:

- `parkingId`
- `vehicleTypeCode`
- `status`
- `search`
- `page`
- `size`

Suggested rule response fields:

- `id`
- `name`
- `parkingId`
- `parkingName`
- `vehicleTypeCode`
- `vehicleTypeName`
- `freeMinutes`
- `firstBlockMinutes`
- `firstBlockPrice`
- `nextBlockMinutes`
- `nextBlockPrice`
- `dailyCapPrice`
- `graceMinutesAfterPayment`
- `status`
- `createdAt`
- `updatedAt`

Suggested preview request fields:

- `checkInAt`
- `checkOutAt`
- `vehicleTypeCode`
- optional `parkingId`

Suggested preview response fields:

- `amount`
- `currency`
- `durationMinutes`
- `chargeableMinutes`
- `freeMinutesApplied`
- `dailyCapApplied`
- `ruleId`
- `ruleName`
- `breakdown`: array of `{ label, minutes, quantity, unitPrice, amount }`

### PWA Checkout Quote

```http
GET /pwa/cards/{qrToken}/checkout-quote
```

Suggested response fields:

- `state`: `ACTIVE_UNPAID` | `PAID` | `COMPLETED` | `NO_ACTIVE_SESSION` | `NO_PRICING_RULE`
- `sessionId`
- `plateNumber`
- `cardCode`
- `parkingName`
- `floorName`
- `zoneName`
- `slotCode`
- `checkInAt`
- `checkOutAt`
- `durationMinutes`
- `amount`
- `currency`
- `pricingRuleId`
- `pricingRuleName`
- `breakdown`
- `quotedAt`
- `message`

## Implementation Phases

### Flow 2A: Pricing Rule Setup + Quote Readiness

1. Add `src/service/manager/pricing-type.ts` and `pricing-api.ts`.
2. Replace `/manager/pricing/time-rules` mock with real rule list/create/edit/status/delete/preview UI.
3. Replace `/manager/pricing/matrix` mock with read-only table view sourced from pricing rules.
4. Add `GET /pwa/cards/{qrToken}/checkout-quote` client types and query.
5. Extend PWA active-session page with checkout quote panel and disabled Pay & Exit CTA.
6. Keep subscriptions, invoices, debts, and payment gateway pages mock/API pending.

### Flow 2B: Staff Exit Cashier

1. Replace `/staff/exit` mock with active-session search.
2. Add checkout quote display for staff cashier.
3. Prepare payment confirmation UI but keep real payment gateway disabled until backend contract is ready.

### Flow 2C: Billing Records

1. Subscriptions.
2. Invoices.
3. Debts/reminders.
4. Payment gateway integration only after pricing and quote contracts are stable.

## Recommended Future Atomic Commits

- `feat(manager-pricing): add pricing rule service contracts`
- `feat(manager-pricing): build time rules CRUD shell`
- `feat(manager-pricing): add pricing preview calculator`
- `feat(manager-pricing): add pricing matrix table`
- `feat(pwa-checkout): add checkout quote service`
- `feat(pwa-checkout): show quote panel on active session`
- `docs(pricing): document billing frontend phases`
