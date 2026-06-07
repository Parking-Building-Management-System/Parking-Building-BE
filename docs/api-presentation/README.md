# SmartPark API Presentation Package

This folder is a ready-to-use API documentation package for the school/customer backend demo. It explains the API system by business story, actor, endpoint group, database impact, and Bruno testing flow.

No secrets are included here. Use placeholder environment variables and short-lived demo tokens only when running the live demo.

## Recommended Reading Order

1. `00-api-grouping-overview.md` - the three presentation groups and speaker split.
2. `01-group-foundation-admin-apis.md` - Group 1 speaker notes for identity, admin, security, and monitoring.
3. `02-group-parking-operations-apis.md` - Group 2 speaker notes for facility setup, staff/kiosk, entry, exit, and PWA guidance.
4. `03-group-payment-safety-apis.md` - Group 3 speaker notes for pricing, PayOS, and Fire Safety.
5. `04-usecase-flow-by-api.md` - outline-ready internal request-to-database flow for important APIs.
6. `07-api-to-database-impact.md` - database impact map for teacher explanation.
7. `05-slide-content-brief.md` - slide content brief for the content team.
8. `06-demo-script.md` - step-by-step live demo script.
9. `08-bruno-testing-guide.md` - Bruno collection usage and variables.

## Who Should Use Which File

- Backend speakers: `00`, `01`, `02`, `03`, `04`, and `07`.
- Slide/content team: `05`.
- Demo operators: `06`.
- API testers: `08`.

## Demo Scope

The package covers completed SmartPark modules:

- Auth, session, device trust, tenant isolation, roles, permissions, audit, and health.
- Manager facility setup: parkings, floors, zones, slots, floor maps, slot pins, RFID cards.
- Manager staff, kiosk/gate setup, staff device approvals.
- Staff entry check-in, Driver PWA session guide, Staff exit preview/complete.
- Pricing rules, checkout quotes, PayOS payment intents/webhooks/status.
- Fire extinguisher inventory, fire-safety map, inspection logs, staff inspection/photo upload.
