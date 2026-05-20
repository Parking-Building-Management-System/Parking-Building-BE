# 07. Sprint 1 Backend Task Map

Tài liệu này map các task Sprint 1 vào code hiện tại. Repo hiện mới có identity/auth/tenant/cache cơ bản. Các phần dashboard, parking building, vehicle category chưa có entity/schema/controller, nên nhiều endpoint/path cần confirm API contract với FE/leader.

## 1. Chốt các table liên quan tới Auth

Mục tiêu: xác nhận schema auth đủ cho login, session, device trust, RBAC, tenant.

Đọc trước:

- `src/main/resources/db/migration/V20260515023500__init_tenant_table.sql`
- `src/main/resources/db/migration/V20260516162100__init_identity_tables.sql`
- `src/main/resources/db/migration/V20260518093000__seed_identity_auth.sql`
- `modules/identity/entity/User.java`
- `Tenant.java`, `Role.java`, `Permission.java`, `UserRole.java`, `RolePermission.java`, `Device.java`, `Session.java`

File/folder nên code: nếu chỉ chốt table thì không code. Nếu cần đổi schema, tạo migration mới trong `src/main/resources/db/migration`, không sửa migration cũ.

DTO cần tạo: không cần nếu chỉ chốt schema.

Repository/query cần tạo: không cần nếu chỉ review.

Service logic cần có: không cần nếu chỉ review.

Controller endpoint đề xuất: không có.

Permission/security cần check:

- Role seed: `SYSTEM_ADMIN`, `PARKING_MANAGER`, `STAFF`, `PARKING_USER`.
- Permission seed đã có nhiều quyền hệ thống/bãi xe.
- Authority runtime prefix: role -> `ROLE_`, permission -> `PERM_`.

Cache cần check/invalidate:

- Authz cache theo session sẽ stale nếu đổi role/permission.
- Cần strategy clear session authz cache khi update RBAC.

Test case nên viết:

- Migration validate app start.
- Query role/permission theo user seed.
- Login với seed approved device.

Checklist hoàn thành:

- Chốt entity/table/field/relation.
- Chốt unique constraints.
- Chốt soft delete/status.
- Chốt seed user/device/role/permission.
- Ghi rõ phần chưa đủ schema.

Prompt mẫu:

```txt
Hãy đọc migration và entity auth hiện tại trong backend SmartPark. Lập bảng so sánh table users/roles/permissions/user_roles/role_permissions/devices/sessions/tenants với entity Java tương ứng. Không sửa code. Nếu thấy mismatch hoặc thiếu schema cho Sprint 1, liệt kê câu hỏi cần confirm với leader.
```

Lỗi dễ gặp: sửa migration cũ, đổi enum string không khớp DB, quên unique `(user_id, fingerprint)`, quên clear authz cache khi đổi quyền.

## 2. Khởi tạo dự án, implement tính năng liên quan tới Security

Mục tiêu: hoàn thiện security foundation, JWT, password, CORS, exception, auth endpoints.

Đọc trước:

- `pom.xml`
- `application.yaml`
- `common/security/config/SecurityConfig.java`
- `common/security/jwt/CustomJwtDecoder.java`
- `modules/identity/controller/AuthenticationController.java`
- `AuthenticationServiceImpl.java`
- `TokenServiceImpl.java`
- `GlobalExceptionHandler.java`

File/folder nên code:

- `common/security/**` cho security core.
- `modules/identity/service/auth/**` cho auth business.
- `modules/identity/controller/AuthenticationController.java` cho endpoint auth.

DTO cần tạo: chỉ tạo thêm nếu API mới như approve device/logout specific session có contract rõ.

Repository/query cần tạo: `SessionRepository`, `DeviceRepository`, `UserRepository` đã có.

Service logic cần có:

- Login check user/password/device.
- Refresh rotate JTI.
- Logout revoke session.
- Resolve roles/permissions.

Controller endpoint đề xuất: đã có `/auth/login`, `/auth/refresh`, `/auth/me`, `/auth/logout`, `/auth/logout-all`, `/auth/admin/users/{userId}/force-logout`.

Permission/security cần check:

- Public endpoints trong `SecurityConfig`.
- `@PreAuthorize` cho admin-only.
- CORS credentials.

Cache cần check/invalidate:

- Session revoked marker.
- Session authz cache.
- Rate limit key.

Test case nên viết:

- Login success/fail.
- Device pending.
- Refresh replay fails.
- Logout makes old access token invalid.
- Force logout requires `SYSTEM_ADMIN`.

Checklist hoàn thành:

- App start với DB/Redis.
- Swagger thấy auth endpoints.
- Seed user login được bằng approved device.
- Refresh cookie/header hoạt động.
- Error format dùng `ApiResponse`.

Prompt mẫu:

```txt
Hãy đọc security/auth hiện tại của SmartPark trước khi code. Task của tôi là hoàn thiện [mô tả]. Không thay đổi public/private endpoint khi chưa cần. Không bịa DTO/entity. Nếu cần thêm config hoặc endpoint, giải thích impact tới FE và Swagger. Sau khi code chạy test auth phù hợp.
```

Lỗi dễ gặp: làm `/auth/me` public, dùng sai `hasRole`, lưu refresh token ở localStorage, không rotate refresh JTI.

## 3. Viết API Dashboard: Thống kê Counters

Mục tiêu: trả các số đếm như tenants, bãi xe, user, session hoặc slot tùy dashboard contract.

Đọc trước:

- `TenantRepository.java`
- Các entity parking nếu được thêm sau.
- `common/response/ApiResponse.java`, `PageResponse.java`
- `infrastructure/cached/redis/keys/RedisKeys.java`

File/folder nên code:

- Cần tạo module mới, ví dụ `modules/dashboard/controller/DashboardController.java`.
- `modules/dashboard/service/DashboardService.java`, `impl/DashboardServiceImpl.java`.
- `modules/dashboard/dto/response/DashboardCountersResponse.java`.

DTO cần tạo:

- `DashboardCountersResponse`, field cần confirm với FE/leader.

Repository/query cần tạo:

- Có thể dùng `TenantRepository.count()`.
- Count bãi xe cần entity parking/building. Chưa có trong code hiện tại.

Service logic cần có:

- Aggregate counts.
- Apply tenant scope nếu user không phải system admin. Cần confirm.

Controller endpoint đề xuất:

- `GET /dashboard/counters` hoặc `GET /admin/dashboard/counters`; cần confirm API contract với FE/leader.

Permission/security cần check:

- Dashboard hệ thống có thể cần `SYSTEM_ADMIN`.
- Dashboard bãi xe có thể cần permission `PERM_QUYEN_TREN_APP.BAI_XE.BAO_CAO.VIEW`.

Cache cần check/invalidate:

- Counter có thể cache TTL ngắn 30-60s.
- Invalidate khi create/update/delete tenant/parking.

Test case nên viết:

- Count đúng với seed.
- User thiếu quyền bị 403.
- Cache miss/hit nếu có.

Checklist hoàn thành:

- Confirm list counters.
- Confirm scope theo tenant/global.
- Response DTO có example Swagger.
- Có test hoặc plan test.

Prompt mẫu:

```txt
Tạo API dashboard counters cho SmartPark. Trước khi code hãy đọc entity/repository hiện có và không bịa bảng parking nếu chưa có. Nếu thiếu entity để count bãi xe, dừng lại liệt kê schema/API cần confirm. Dùng Controller-Service-Repository-DTO-ApiResponse, thêm permission và cache nếu phù hợp.
```

Lỗi dễ gặp: count global trong khi user tenant chỉ được xem tenant mình, bịa entity parking, cache counter không invalidate.

## 4. Viết API Dashboard: Lấy data biểu đồ Traffic

Mục tiêu: trả data chart-friendly theo time range.

Đọc trước:

- Entity traffic/parking session nếu được thêm sau.
- Migration liên quan lượt gửi xe nếu có sau này.
- `PageResponse` nếu cần paging.

File/folder nên code:

- `modules/dashboard/controller/DashboardController.java`.
- `modules/dashboard/dto/request/TrafficChartRequest` hoặc dùng query params.
- `modules/dashboard/dto/response/TrafficChartResponse`.

DTO cần tạo:

- Request/query: `from`, `to`, `granularity` nếu FE cần.
- Response: labels/buckets/series. Cần confirm format với FE.

Repository/query cần tạo:

- Query aggregation theo ngày/giờ.
- Hiện chưa có bảng traffic/parking sessions, cần confirm schema.

Service logic cần có:

- Validate time range.
- Convert timezone rõ ràng.
- Aggregate theo tenant/building nếu có.

Controller endpoint đề xuất:

- `GET /dashboard/traffic` hoặc `GET /dashboard/traffic-chart`; cần confirm API contract với FE/leader.

Permission/security cần check:

- `BAO_CAO.VIEW` hoặc role manager/admin.

Cache cần check/invalidate:

- Cache theo `(tenantId, from, to, granularity)`.
- TTL ngắn nếu dữ liệu realtime.

Test case nên viết:

- Empty range trả series rỗng.
- Invalid date range -> 400.
- User thiếu quyền -> 403.

Checklist hoàn thành:

- Confirm bảng nguồn traffic.
- Confirm timezone.
- Confirm response chart format.
- Swagger example đủ cho FE.

Prompt mẫu:

```txt
Tạo API traffic chart cho dashboard SmartPark. Trước khi code hãy tìm entity/migration lưu lượt xe/traffic. Nếu chưa có schema, dừng và hỏi. Không bịa field thời gian/trạng thái. Response phải chart-friendly và có Swagger example.
```

Lỗi dễ gặp: timezone sai, group by sai granularity, trả format khó vẽ chart.

## 5. Viết API Tenant: Lấy danh sách khách hàng

Mục tiêu: list tenants có pagination/sort/filter.

Đọc trước:

- `TenantController.java`
- `TenantService.java`, `TenantServiceImpl.java`
- `TenantRepository.java`
- `Tenant.java`, `TenantResponse.java`
- `PageResponse.java`

File/folder nên code:

- Update `TenantController`, `TenantService`, `TenantServiceImpl`, `TenantRepository`.
- Có thể tạo `dto/tenant/request/TenantSearchRequest` nếu không dùng query params trực tiếp.

DTO cần tạo:

- Có thể tái dùng `TenantResponse`.
- Response paging: `ApiResponse<PageResponse<TenantResponse>>`.

Repository/query cần tạo:

- `Page<Tenant> findAll(Pageable pageable)`.
- Nếu filter name/slug/status, cân nhắc `JpaSpecificationExecutor<Tenant>`.

Service logic cần có:

- Validate page/size/sort.
- Whitelist sort field.
- Map entity -> response.

Controller endpoint đề xuất:

- `GET /tenants?page=0&size=20&sort=createdAt,desc&keyword=...&status=ACTIVE`.

Permission/security cần check:

- Nên `SYSTEM_ADMIN` hoặc permission tenant view. Hiện controller chưa enforce, cần confirm.

Cache cần check/invalidate:

- List cache chỉ thêm nếu cần. Nếu cache list, invalidate khi create/suspend/delete tenant.

Test case nên viết:

- Paging metadata đúng.
- Filter status.
- Sort invalid -> `INVALID_SORT_FIELD`.

Checklist hoàn thành:

- Contract paging confirm với FE.
- Không trả tenant soft-deleted do `@SQLRestriction`.
- Swagger có query params.

Prompt mẫu:

```txt
Thêm API GET list tenants có pagination/filter/sort. Đọc TenantController/Service/Repository/Entity/PageResponse trước. Không trả Entity. Dùng ApiResponse<PageResponse<TenantResponse>>. Nếu cần Specification thì thêm đúng layer. Thêm permission theo convention hiện tại hoặc dừng hỏi nếu chưa rõ.
```

Lỗi dễ gặp: page 1-based/0-based không thống nhất, sort field không whitelist, thiếu `@PreAuthorize`.

## 6. Viết API Tenant: Tạo tenant mới

Mục tiêu: tạo tenant mới với validation và unique slug.

Đọc trước:

- `TenantController.createTenant`
- `TenantServiceImpl.createTenant`
- `TenantCreationRequest`
- `TenantRepository.existsBySlug`
- `TenantMapper`

File/folder nên code: hiện đã có các file cần thiết. Chỉ sửa khi yêu cầu mới.

DTO cần tạo:

- Hiện có `TenantCreationRequest`.
- Nếu đổi naming, cần cân nhắc backward compatibility.

Repository/query cần tạo: đã có `existsBySlug`.

Service logic cần có:

- Check duplicate slug.
- Set status `ACTIVE`.
- Save trong `@Transactional`.
- Map response.

Controller endpoint đề xuất: đã có `POST /tenants`.

Permission/security cần check: cần thêm `@PreAuthorize` nếu business yêu cầu System Admin.

Cache cần check/invalidate: nếu có list/cache counters thì invalidate sau create. Hiện create chưa evict list vì chưa có list cache.

Test case nên viết:

- Valid create.
- Duplicate slug -> 409.
- Invalid email/slug -> 400.

Checklist hoàn thành:

- Validation OK.
- Duplicate handled.
- Swagger example.
- Permission confirmed.

Prompt mẫu:

```txt
Review và hoàn thiện API create tenant hiện tại. Đọc TenantController/Service/DTO/Entity/migration. Không đổi response field nếu FE đã dùng. Nếu thêm permission, dùng @PreAuthorize theo authority hiện tại và nêu rõ impact.
```

Lỗi dễ gặp: không normalize slug, duplicate race condition chỉ check service mà không handle DB exception, thiếu permission.

## 7. Viết API Tenant: Update trạng thái

Mục tiêu: update tenant status active/suspended.

Đọc trước:

- `TenantController.suspendTenant`
- `TenantServiceImpl.suspendTenant`
- `TenantStatus.java`
- `TenantCacheService.java`

File/folder nên code:

- `TenantController`
- `TenantService`
- `TenantServiceImpl`
- Có thể tạo `TenantStatusUpdateRequest`.

DTO cần tạo:

- `TenantStatusUpdateRequest` với `TenantStatus status` nếu cần active lại.

Repository/query cần tạo: `findById` đủ cho basic.

Service logic cần có:

- Find tenant.
- Validate status transition nếu có rule.
- Update status.
- Invalidate tenant cache.
- Nếu suspend tenant phải revoke/kick toàn bộ user thuộc tenant: hiện comment Swagger nói kick Redis nhưng code chỉ evict tenant cache, chưa revoke session user thuộc tenant. Cần kiểm tra lại trong code hiện tại và hỏi leader/backend owner.

Controller endpoint đề xuất:

- Hiện có `PATCH /tenants/{id}/suspend`.
- Nếu generic: `PATCH /tenants/{id}/status`, cần confirm FE.

Permission/security cần check:

- Nên admin-only.

Cache cần check/invalidate:

- `TenantCacheService.evictTenantData(id)`.
- Auth/session cache của users thuộc tenant nếu tenant bị suspend. Chưa có query/service làm việc này.

Test case nên viết:

- Suspend tenant.
- Tenant detail cache bị clear.
- User thuộc tenant không login được sau suspend.
- Active token của user thuộc tenant còn dùng được hay không: cần confirm expected behavior.

Checklist hoàn thành:

- Confirm endpoint.
- Confirm active lại có cần API không.
- Confirm revoke sessions khi suspend.

Prompt mẫu:

```txt
Thêm/hoàn thiện API update tenant status. Đọc TenantStatus, TenantServiceImpl, TenantCacheService và auth session flow. Không bịa behavior kick session. Nếu code hiện tại chưa revoke session theo tenant, dừng hỏi leader backend owner trước khi implement.
```

Lỗi dễ gặp: chỉ đổi DB status nhưng session cũ vẫn active, không invalidate tenant cache.

## 8. Viết API: Tạo mới, đọc loại phương tiện

Mục tiêu: CRUD phần Vehicle Category.

Đọc trước:

- Tìm `VehicleCategory` entity/migration. Hiện chưa thấy trong code hiện tại.
- Permission seed có `QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE`.

File/folder nên code:

- Cần tạo module mới sau khi confirm schema, ví dụ `modules/vehicle`.
- Entity/repository/service/controller/dto/mapper tương ứng.

DTO cần tạo:

- `CreateVehicleCategoryRequest`
- `VehicleCategoryResponse`
- Có thể cần `VehicleCategoryListResponse` hoặc `PageResponse`.

Repository/query cần tạo:

- `VehicleCategoryRepository extends JpaRepository<VehicleCategory, UUID>`.
- Unique code/name nếu schema yêu cầu. Cần confirm.

Service logic cần có:

- Validate duplicate.
- Save.
- Read detail/list.
- Soft delete rule nếu có.

Controller endpoint đề xuất:

- `POST /vehicle-categories`
- `GET /vehicle-categories`
- `GET /vehicle-categories/{id}`
- Cần confirm API contract với FE/leader.

Permission/security cần check:

- Create/update/delete: `PERM_QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE`.
- Read có thể cần VIEW hoặc MANAGE; seed hiện chỉ thấy MANAGE cho loại xe.

Cache cần check/invalidate:

- Cache list category nếu ít thay đổi.
- Invalidate list/detail sau create/update/delete.

Test case nên viết:

- Create valid.
- Duplicate.
- Get list.
- User thiếu quyền.

Checklist hoàn thành:

- Confirm table fields.
- Confirm enum/status.
- Migration mới.
- Swagger example.

Prompt mẫu:

```txt
Tạo API create/read vehicle category. Trước khi code hãy tìm entity/migration VehicleCategory. Nếu chưa có, dừng và liệt kê schema cần leader confirm. Sau khi có schema, tạo đúng layer Controller-Service-Repository-Entity-DTO-Mapper, không trả Entity, thêm permission LOAI_XE.MANAGE.
```

Lỗi dễ gặp: tự bịa field `code/name/status`, hard delete khi business cần soft delete.

## 9. Viết API: Update, Delete phương tiện

Mục tiêu: update/delete vehicle hoặc vehicle category theo task ownership thực tế.

Đọc trước:

- Entity vehicle/category sau khi có.
- Migration relation tới parking session/ticket nếu có.
- Permission seed liên quan loại xe.

File/folder nên code:

- Module vehicle/category đã tạo ở task 8.

DTO cần tạo:

- `UpdateVehicleCategoryRequest` hoặc `UpdateVehicleRequest`.
- Response DTO tái dùng.

Repository/query cần tạo:

- `existsBy...` cho duplicate.
- Query check relation đang được dùng nếu không cho delete.

Service logic cần có:

- Find by id.
- Validate relation constraints.
- Update fields.
- Delete soft hay hard phải confirm.

Controller endpoint đề xuất:

- `PUT /vehicle-categories/{id}` hoặc `PATCH /vehicle-categories/{id}`.
- `DELETE /vehicle-categories/{id}`.
- Cần confirm API contract với FE/leader.

Permission/security cần check:

- `PERM_QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE`.

Cache cần check/invalidate:

- Clear detail/list/category cache.
- Clear dashboard/price cache nếu category ảnh hưởng pricing.

Test case nên viết:

- Update valid.
- Delete entity đang được reference.
- Delete not found.
- Permission 403.

Checklist hoàn thành:

- Confirm soft/hard delete.
- Confirm relation constraints.
- Cache invalidation.

Prompt mẫu:

```txt
Thêm update/delete cho vehicle category. Đọc entity/repository/migration trước. Không quyết định soft delete hay hard delete nếu chưa có convention; hỏi leader. Không xóa dữ liệu có relation nếu DB/business không cho phép. Dùng ApiResponse và AppException/ErrorCode.
```

Lỗi dễ gặp: hard delete làm vỡ FK, update code unique trùng, không invalidate cache.

## 10. Viết Docs API để FE tích hợp

Mục tiêu: mỗi API có Swagger/OpenAPI và docs contract cho FE.

Đọc trước:

- `SwaggerConfig.java`
- `AuthenticationController` để xem cách dùng `@Operation`, `@ApiResponses`, `@ExampleObject`.
- `docs/10-fe-integration-contract.md`

File/folder nên code:

- Controller Java: thêm annotation OpenAPI.
- Docs markdown trong `docs/` nếu cần contract thủ công.

DTO cần tạo: example request/response nếu chưa rõ.

Repository/query cần tạo: không.

Service logic cần có: không.

Controller endpoint đề xuất: annotate endpoint hiện có.

Permission/security cần check: docs phải ghi rõ auth/role/permission.

Cache cần check/invalidate: docs phải ghi rõ cache behavior nếu ảnh hưởng FE.

Test case nên viết:

- Mở `/swagger-ui/index.html`.
- Gọi endpoint sample bằng seed user.

Checklist hoàn thành:

- Method + URL.
- Request body/query params.
- Success/error response.
- Error code.
- Auth/permission.
- Enum values.
- Date/time format.

Prompt mẫu:

```txt
Viết Swagger/OpenAPI docs cho endpoint [X]. Đọc controller/DTO/ErrorCode/SecurityConfig. Không đổi business logic. Thêm summary, description, request/response example, error response, auth requirement. Tóm tắt contract cho FE.
```

Lỗi dễ gặp: Swagger nói admin-only nhưng code chưa `@PreAuthorize`, example field khác DTO thật.

## 11. Viết API: Create, Read quyền

Mục tiêu: quản lý permission.

Đọc trước:

- `Permission.java`
- `PermissionRepository.java`
- `RolePermission.java`
- `V20260518093000__seed_identity_auth.sql`
- `JwtAuthenticationConverter.java`
- `SessionAuthorityCacheService.java`

File/folder nên code:

- `modules/identity/controller/PermissionController.java`
- `modules/identity/service/PermissionService.java`
- `modules/identity/service/impl/PermissionServiceImpl.java`
- `modules/identity/dto/permission/request`
- `modules/identity/dto/permission/response`
- `modules/identity/mapper/PermissionMapper.java`

DTO cần tạo:

- `CreatePermissionRequest`: `name`, `scope`, `module`, `resource`, `label`, `action`.
- `PermissionResponse`.

Repository/query cần tạo:

- `existsByName` cần thêm vào `PermissionRepository`.
- List/read queries nếu cần paging.

Service logic cần có:

- Validate unique name.
- Create permission.
- Read detail/list.
- Không tự gán permission vào role nếu task không yêu cầu.

Controller endpoint đề xuất:

- `POST /permissions`
- `GET /permissions`
- `GET /permissions/{id}`
- Cần confirm API contract với FE/leader.

Permission/security cần check:

- Rất nhạy cảm. Nên chỉ `SYSTEM_ADMIN` hoặc `PERM_QUYEN_TREN_APP.HE_THONG.PERMISSION.MANAGE`.

Cache cần check/invalidate:

- Create permission chưa ảnh hưởng user authz nếu chưa gán role.
- Nếu gán role sau đó, phải clear authz cache user liên quan.

Test case nên viết:

- Create duplicate.
- Read list paging.
- Non-admin forbidden.

Checklist hoàn thành:

- Confirm ai được tạo permission.
- Confirm naming convention permission.
- Swagger example.

Prompt mẫu:

```txt
Tạo API create/read permission trong module identity. Đọc Permission entity/migration/seed và security authority mapping trước. Đây là API nhạy cảm, không mở public. Không bịa field ngoài entity hiện có. Nếu cần endpoint/path exact thì hỏi FE/leader.
```

Lỗi dễ gặp: cho user thường tạo permission, permission name không theo convention, không tính cache RBAC.

## 12. Viết API: Update, Delete quyền

Mục tiêu: cập nhật/xóa permission an toàn.

Đọc trước:

- `Permission.java`
- `RolePermission.java`
- `PermissionRepository.java`
- `V20260518093000__seed_identity_auth.sql`
- `SessionAuthorityResolver.java`

File/folder nên code:

- Permission controller/service/dto/mapper đã tạo ở task 11.

DTO cần tạo:

- `UpdatePermissionRequest`.

Repository/query cần tạo:

- Check permission đang gắn role qua `RolePermission`.
- Có thể cần `RolePermissionRepository`; hiện chưa có trong code. Cần tạo nếu task yêu cầu và confirm ownership.

Service logic cần có:

- Find permission.
- Không cho xóa permission system critical nếu business yêu cầu. Hiện chưa có flag system critical trong schema, cần hỏi leader/backend owner.
- Update unique name an toàn.
- Delete: soft/hard cần confirm vì table không có `is_deleted`.
- Invalidate authz cache user bị ảnh hưởng nếu permission đổi/xóa.

Controller endpoint đề xuất:

- `PUT /permissions/{id}` hoặc `PATCH /permissions/{id}`.
- `DELETE /permissions/{id}`.
- Cần confirm API contract với FE/leader.

Permission/security cần check:

- Chỉ admin hoặc permission manage.

Cache cần check/invalidate:

- Bắt buộc xử lý authz cache stale.
- Cần biết user nào bị ảnh hưởng qua role_permission -> user_roles.

Test case nên viết:

- Update name duplicate.
- Delete permission đang gắn role.
- User đang login mất quyền sau update/delete.

Checklist hoàn thành:

- Confirm soft/hard delete.
- Confirm system critical permission.
- Implement invalidation hoặc force logout strategy.

Prompt mẫu:

```txt
Thêm update/delete permission. Đọc RBAC entity/migration/seed và session authz cache. Không xóa permission critical nếu schema/business chưa rõ; dừng hỏi leader. Nếu permission đổi ảnh hưởng user đang login, thiết kế invalidate authz cache hoặc force logout, không bỏ qua.
```

Lỗi dễ gặp: xóa permission đang dùng làm vỡ role_permission, cache quyền stale, mở API cho sai role.
