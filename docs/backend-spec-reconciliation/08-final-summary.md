# 08. Final Summary

## 1. Backend hiện tại mạnh ở đâu

- Auth/session foundation khá tốt: username/password, JWT access/refresh, refresh JTI rotation, DB session, Redis authz cache, revoked marker, logout/logout-all/force logout.
- Có SaaS tenant/admin nền tảng: `/admin/tenants`, `/admin/dashboard/stats`, vehicle type CRUD, role list.
- Core facility data đã có: `parkings`, `floors`, `zones`, `slots`, tenant_id trực tiếp, manager floor/zone APIs, slot search/import/export.
- Redis cache đã có cho session, tenant detail, admin dashboard, vehicle types, parking topology, rate limit.
- Hibernate tenant filter foundation đã có.

## 2. Backend hiện tại yếu ở đâu

- Tenant isolation chưa được bind global cho mọi non-admin request; hiện manager facility tự wrap context, còn future staff/user dễ quên.
- Operation flow chưa có API: entry, exit, fast cash, bill, release slot, move vehicle.
- Billing/payment/pricing thiếu service/API; pricing và payment entity còn MISSING.
- Device binding có login block pending nhưng chưa có manager approval/revoke API.
- Staff account management chưa có manager CRUD.
- Red flag/incident/blind drop chỉ có vài entity nền, chưa có workflow.
- PWA/lazy auth/exit pass chưa có.

## 3. Top 10 gap lớn nhất so với spec

1. Global tenant context binding/tests MISSING/PARTIAL.
2. Manager staff CRUD MISSING.
3. Device approval/revoke/temporary 8h APIs MISSING.
4. Staff entry operations MISSING.
5. Staff exit/cashier fast cash MISSING.
6. PricingPolicy/PricingMatrix/PricingTimeRule MISSING.
7. Payment entity/API and payment webhook workflow MISSING.
8. RedFlagAction/Incident workflow MISSING.
9. StaffShift/ShiftCashDrop blind drop MISSING.
10. PWA active session/checkout/lazy auth/exit pass MISSING.

## 4. Rủi ro lớn nhất nếu code CRUD ngay

Nếu code thêm manager/staff/user CRUD trước khi chốt tenant foundation, chỉ cần một controller không set `TenantContext` là Hibernate filter bị disable và repository có thể đọc toàn cục. Đây là rủi ro cross-tenant leak lớn nhất, đặc biệt với biển số/session/invoice.

## 5. Thứ tự khuyến nghị

- Phase 0 foundation: tenant context binding global cho non-admin, admin bypass rõ ràng, isolation tests.
- Phase 1 CRUD basic: admin tenant/vehicle type ổn định; manager parking/floor/zone/slot; manager staff; device approval; kill switch.
- Phase 2 operations: entry/exit, parking session, card, slot assignment, red flag, incident, blind drop.
- Phase 3 billing/payment: pricing rules/matrix, invoices, payments, subscription jobs, debt notification.
- Phase 4 PWA: active session, online checkout, exit pass QR, subscription/invoice, lazy auth.

## 6. Task Codex đầu tiên nên chạy tiếp

Task 3: Implement tenant context and isolation foundation. Đây là dependency an toàn cho mọi CRUD tenant-scoped sau đó.

## 7. Những file docs quan trọng nhất vừa tạo

- `docs/backend-spec-reconciliation/00-current-backend-state.md`
- `docs/backend-spec-reconciliation/01-spec-vs-code-gap.md`
- `docs/backend-spec-reconciliation/03-multi-tenant-isolation-plan.md`
- `docs/backend-spec-reconciliation/04-crud-first-phase-scope.md`
- `docs/backend-spec-reconciliation/05-backend-task-prompts.md`
