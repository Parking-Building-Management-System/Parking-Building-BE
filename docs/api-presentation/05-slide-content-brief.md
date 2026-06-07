# Slide Content Brief

Use English slide headings. Vietnamese explanations can be used in speaker notes if the class presentation is in Vietnamese.

## 1. System API Architecture Overview

Message: SmartPark backend is organized by actors and business workflows.

- REST API with Spring Boot.
- JWT bearer authentication for protected APIs.
- Public QR-token APIs for Driver PWA.
- PayOS webhook endpoint for provider callback.
- Tenant-scoped domain data.
- Bruno collection and Swagger/OpenAPI for testing.

Vietnamese note: "API không chỉ CRUD; API thay đổi trạng thái bãi xe, thanh toán, thiết bị và kiểm định an toàn."

## 2. Three API Groups and Why

Group 1 - Foundation, Identity & Administration:

- Login, refresh, profile.
- Tenant provisioning.
- Master data, roles, permissions.
- Audit, sessions, devices, health.

Group 2 - Parking Facility & Daily Operations:

- Parking/floor/zone/slot setup.
- Floor maps and slot pins.
- RFID cards.
- Staff, kiosk, device approval.
- Staff entry/exit and Driver PWA active-session guide.

Group 3 - Pricing, Payment & Safety Compliance:

- Pricing rules and quote preview.
- PWA checkout quote and PayOS payment.
- Webhook, payment status, exit deadline.
- Fire extinguisher map and inspection logs.

## 3. Main Actor Map

- System Admin: platform owner, manages tenants/security/health.
- Parking Manager: tenant admin, configures facility, people, devices, pricing, safety.
- Staff: gate operator using trusted kiosk devices.
- Driver/PWA: scans RFID card QR for map, quote, and payment.
- PayOS: external payment provider calling webhook.

## 4. End-To-End Demo Flow

1. Manager setup: parking, floor, zone, slot, map, RFID card, staff, kiosk.
2. Staff entry: trusted staff logs in and checks in a vehicle.
3. Driver PWA: QR opens assigned slot map.
4. Payment: PWA quote, PayOS intent, webhook/status.
5. Staff exit: preview and complete exit, slot released.
6. Fire safety: manager map, staff inspection/photo, manager logs.

## 5. Security Highlights

- JWT access token and refresh token rotation.
- Role-based access: `SYSTEM_ADMIN`, `PARKING_MANAGER`, `STAFF`.
- Tenant isolation from JWT tenant context.
- Device trust for manager/staff login.
- Staff kiosk context controls entry/exit permission.
- PayOS webhook verification by checksum signature.
- No secrets in docs, Bruno, or Swagger examples.

## 6. Database Consistency Highlights

- Tenant-scoped tables for business data.
- Facility hierarchy: parking -> floor -> zone -> slot.
- Check-in state transition: slot `AVAILABLE -> OCCUPIED`, session `ACTIVE`.
- Payment transition: intent `PENDING -> PAID`, session payment `PAID`, exit deadline set.
- Exit transition: session `ACTIVE -> COMPLETED`, slot `OCCUPIED -> AVAILABLE`.
- Fire inspection transition: inspection log created, extinguisher last/next inspection updated.
- Idempotent/reusable patterns: refresh JTI rotation, pending payment intent reuse, already-paid webhook handling.

## 7. Suggested Slide Outline

### Slide 1: Problem & Actors

Content:

- Parking buildings need secure entry/exit, payment, facility maps, and safety checks.
- Actors: System Admin, Manager, Staff, Driver/PWA, PayOS.

Speaker notes:

- "SmartPark connects admin control, parking operation, payment, and safety compliance in one API system."

### Slide 2: API Grouping

Content:

- Group 1: Foundation/Admin.
- Group 2: Parking Operations.
- Group 3: Payment/Safety.

Speaker notes:

- "We split APIs this way so the demo follows the real workflow instead of random endpoints."

### Slide 3: Group 1 Foundation/Admin

Content:

- Login/refresh/me.
- Tenant provisioning.
- Roles/permissions.
- Health, audit, sessions, devices.

Speaker notes:

- "This layer answers who can access the platform and how admins monitor risk."

### Slide 4: Group 2 Parking Operations

Content:

- Facility hierarchy.
- Map and slot pins.
- RFID cards.
- Staff, kiosks, trusted devices.
- Entry, PWA guide, exit.

Speaker notes:

- "This is the operational lifecycle: configure parking, receive vehicle, guide driver, release slot."

### Slide 5: Group 3 Payment & Fire Safety

Content:

- Pricing rules.
- Checkout quote.
- PayOS payment intent/webhook.
- Exit grace deadline.
- Fire extinguisher map and inspection logs.

Speaker notes:

- "This group shows money and compliance: payment must be verified, and safety checks must be recorded."

### Slide 6: End-To-End Flow

Content:

- Manager setup -> Staff entry -> Driver PWA -> Payment -> Staff exit -> Fire inspection.

Speaker notes:

- "Every step is backed by API calls and database state transitions."

### Slide 7: Security & Data Integrity

Content:

- JWT, RBAC, tenant isolation.
- Device trust and kiosk context.
- Webhook verification.
- State transitions and idempotency.

Speaker notes:

- "The system prevents wrong tenant access, untrusted staff devices, wrong kiosk actions, and fake payment callbacks."

### Slide 8: Demo Script

Content:

- UI demo flow.
- API/Bruno fallback flow.
- Known demo issues.

Speaker notes:

- "If UI is unstable, Bruno can prove the backend flow directly."

### Slide 9: Testing With Bruno

Content:

- Collection folder: `bruno/SmartPark`.
- Local environment on `http://localhost:8081`.
- Token variables and flow variables.

Speaker notes:

- "Bruno lets us run the same API flow deterministically and copy IDs between steps."

### Slide 10: Conclusion

Content:

- Secure multi-tenant backend.
- Complete parking operation lifecycle.
- Payment and fire safety compliance.
- Swagger + Bruno + docs ready for demo.

Speaker notes:

- "The backend demonstrates both business workflows and real data consistency."
