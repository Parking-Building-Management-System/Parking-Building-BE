# Swagger Endpoint Test Plan

Mục tiêu: test thủ công trên Swagger UI để bao phủ các nhánh xử lý hiện có trong code cho
authentication, session authorization và tenant management.

Swagger local: `http://localhost:8080/swagger-ui/index.html`

## 1. Phạm vi endpoint

Hiện dự án có các controller public trên Swagger:

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `POST /auth/admin/users/{userId}/force-logout`
- `GET /dashboard/counters`
- `GET /dashboard/traffic`
- `POST /tenants`
- `GET /tenants/{id}`
- `PATCH /tenants/{id}/status`
- `PATCH /tenants/{id}/suspend`
- `DELETE /tenants/{id}`
- `PATCH /vehicle-types/{id}`
- `DELETE /vehicle-types/{id}`

Lưu ý hiện trạng code mới: nhóm `/tenants/**` và `/dashboard/**` yêu cầu role `SYSTEM_ADMIN`.
Nhóm `/vehicle-types/**` yêu cầu permission `PERM_QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE`, nên
`SYSTEM_ADMIN` và `PARKING_MANAGER` theo seed hiện tại đều gọi được.

## 2. Chuẩn bị dữ liệu

Chạy app với Postgres, Redis và Flyway migration. Seed auth tạo các tài khoản sau, cùng password:
`Password@123`.

| User | Approved device fingerprint | Role chính |
| --- | --- | --- |
| `system.admin@smartpark.local` | `seed-system-admin-device` | `SYSTEM_ADMIN` |
| `manager@demo-parking.local` | `seed-manager-device` | `PARKING_MANAGER` |
| `staff@demo-parking.local` | `seed-staff-device` | `STAFF` |
| `driver@demo-parking.local` | `seed-driver-device` | `PARKING_USER` |

SQL tra cứu ID khi cần nhập path variable:

```sql
select id, username, status from users order by username;
select id, slug, status, is_deleted from tenants order by slug;
select id, name, status, is_deleted from parkings order by name;
select id, name, code, is_active, is_deleted from vehicle_types order by code;
select date_trunc('hour', occurred_at) bucket, count(*) from api_traffic_logs group by bucket order by bucket desc;
select id, user_id, fingerprint, status from devices order by fingerprint;
select id, user_id, refresh_jti, revoked_at, expired_at from sessions order by created_at desc;
```

SQL reset nhanh trước khi chạy lại toàn bộ plan:

```sql
update tenants
set status = 'ACTIVE', is_deleted = false
where slug in ('smartpark-saas', 'demo-parking-tower');

update users
set status = 'ACTIVE', is_deleted = false
where username in (
  'system.admin@smartpark.local',
  'manager@demo-parking.local',
  'staff@demo-parking.local',
  'driver@demo-parking.local'
);

update devices
set status = 'APPROVED'
where fingerprint in (
  'seed-system-admin-device',
  'seed-manager-device',
  'seed-staff-device',
  'seed-driver-device'
);

delete from sessions;
delete from devices where fingerprint like 'swagger-%';
delete from api_traffic_logs;
```

## 3. Cách thao tác token trên Swagger

1. Gọi `POST /auth/login`.
2. Copy `result.accessToken`, bấm `Authorize`, nhập `Bearer <accessToken>`.
3. Với `POST /auth/refresh`, dùng header `X-Refresh-Token` bằng `result.refreshToken` trả về từ
   login hoặc refresh trước đó. Không phụ thuộc cookie của browser để dễ kiểm soát replay case.
4. Khi test nhiều user, lưu riêng biến thủ công: `adminAccess`, `adminRefresh`, `managerAccess`,
   `staffAccess`, `driverAccess`.

Response thành công có `code = 1000`. Response lỗi dùng `ApiResponse` với `result = null`, thường
có `path`, `timestamp`, và có `errors` cho lỗi validation.

## 4. Authentication

### `POST /auth/login`

| Case | Request | Kỳ vọng |
| --- | --- | --- |
| A01 | Login admin với username/password đúng và `seed-system-admin-device` | `200`, `code=1000`, `authenticated=true`, có access token, refresh token và `Set-Cookie: refresh_token` |
| A02 | Login manager/staff/driver với approved device tương ứng | `200`, tạo session riêng cho từng user |
| A03 | Thiếu `username`, `password` hoặc `deviceFingerprint` | `400`, `code=4000`, `errors` có field bị thiếu |
| A04 | JSON sai format | `400`, `code=4001` |
| A05 | Username không tồn tại | `400`, `code=4003`, message sai thông tin đăng nhập |
| A06 | Password sai | `400`, `code=4003` |
| A07 | Device lạ, ví dụ `swagger-new-device-001` | `400`, `code=4005`; DB tạo device `PENDING`, không trả token |
| A08 | Approve device ở A07 rồi login lại | `200`, tạo session/token |
| A09 | Set approved device thành `REJECTED` hoặc `SUSPENDED`, rồi login | `400`, `code=4005` |
| A10 | Set user thành `SUSPENDED` hoặc `INACTIVE`, rồi login | `403`, `code=4030` |
| A11 | Suspend tenant của manager/staff/driver, rồi login user thuộc tenant đó | `403`, `code=4030` |
| A12 | Gửi quá 5 request login trong 60 giây | request tiếp theo `429`, `code=4290` |

SQL hỗ trợ case A08-A11:

```sql
update devices
set status = 'APPROVED', approved_at = current_timestamp
where fingerprint = 'swagger-new-device-001';

update devices set status = 'REJECTED' where fingerprint = 'seed-staff-device';
update devices set status = 'APPROVED' where fingerprint = 'seed-staff-device';

update users set status = 'SUSPENDED' where username = 'staff@demo-parking.local';
update users set status = 'ACTIVE' where username = 'staff@demo-parking.local';

update tenants set status = 'SUSPENDED' where slug = 'demo-parking-tower';
update tenants set status = 'ACTIVE' where slug = 'demo-parking-tower';
```

Ghi chú khi test rate limit: annotation đang khai báo `fieldName = "username"` trong khi aspect parse
như SpEL. Nếu thấy rate limit gom về cùng một key thay vì theo từng username, ghi nhận là mismatch
giữa intent và implementation.

### `POST /auth/refresh`

| Case | Request | Kỳ vọng |
| --- | --- | --- |
| R01 | Header `X-Refresh-Token` bằng refresh token hợp lệ | `200`, trả access token mới, refresh token mới, `Set-Cookie` mới |
| R02 | Không gửi cookie/header refresh token | `401`, `code=4010` |
| R03 | Gửi chuỗi không phải JWT | `401`, `code=4010` |
| R04 | Gửi access token vào `X-Refresh-Token` | `401`, `code=4010` vì thiếu `typ=REFRESH` |
| R05 | Sau R01, dùng lại refresh token cũ | `401`, `code=4010`, cover refresh replay/compare-and-set JTI |
| R06 | Logout session rồi refresh bằng refresh token của session đó | `401`, `code=4010` |
| R07 | Logout-all hoặc force-logout user rồi refresh token cũ | `401`, `code=4010` |
| R08 | Set `sessions.expired_at` về quá khứ rồi refresh | `401`, `code=4010` |
| R09 | Sửa 1 ký tự trong refresh token | `401`, `code=4010` |

SQL hỗ trợ R08:

```sql
update sessions
set expired_at = current_timestamp - interval '1 minute'
where id = '<session-id-can-test>';
```

### `GET /auth/me`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| M01 | Không Authorize Bearer token | `401`, `code=4010` |
| M02 | Bearer token sai chữ ký/sai format | `401`, `code=4010` |
| M03 | Token admin hợp lệ | `200`, trả profile, `roles` có `SYSTEM_ADMIN`, permissions có quyền hệ thống |
| M04 | Token manager/staff/driver hợp lệ | `200`, roles/permissions đúng theo seed |
| M05 | Logout session hiện tại, rồi dùng access token cũ gọi `/auth/me` | `401`, session revoked |
| M06 | Login cùng user 2 lần, gọi logout-all bằng token thứ nhất, token thứ hai gọi `/auth/me` | `401`, toàn bộ session user bị revoke |
| M07 | Admin force-logout target user, target dùng access token cũ gọi `/auth/me` | `401` |

## 5. Logout và force logout

### `POST /auth/logout`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| L01 | Không Bearer token | `401`, `code=4010` |
| L02 | Bearer token hợp lệ | `200`, `code=1000`, `Set-Cookie refresh_token` max-age `0` |
| L03 | Gọi lại bằng access token vừa logout | `401`, session đã revoked |
| L04 | Sau logout, dùng refresh token cũ gọi `/auth/refresh` | `401` |

### `POST /auth/logout-all`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| LA01 | Không Bearer token | `401`, `code=4010` |
| LA02 | Login cùng user nhiều lần, gọi logout-all | `200`, clear cookie |
| LA03 | Các access token/refresh token cũ của user sau LA02 | `/auth/me` và `/auth/refresh` đều `401` |

### `POST /auth/admin/users/{userId}/force-logout`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| F01 | Admin token, `userId` là user đang có session | `200`; token cũ của target gọi `/auth/me` bị `401` |
| F02 | Manager/staff/driver token gọi endpoint này | `403`, `code=4030` |
| F03 | Không Bearer token | `401`, `code=4010` |
| F04 | `userId` không đúng UUID format | `400`, thường map về `code=4999` |
| F05 | UUID hợp lệ nhưng không tồn tại hoặc không có active session | `200`, vì service hiện revoke idempotent theo userId |

## 6. Tenant Management

Trước nhóm case này, login admin và Authorize bằng admin access token. Test thêm bằng manager để
xác nhận các endpoint tenant trả `403`.

### `POST /tenants`

| Case | Request | Kỳ vọng |
| --- | --- | --- |
| T01 | Không Bearer token | `401`, `code=4010` |
| T02 | Body hợp lệ, slug mới như `swagger-tenant-001` | `200`, `code=1000`, `status=ACTIVE` |
| T03 | `name` blank | `400`, `code=4000`, `errors.name` |
| T04 | `slug` blank | `400`, `code=4000`, `errors.slug` |
| T05 | `slug` có chữ hoa/ký tự ngoài `[a-z0-9-]` | `400`, `code=4000`, `errors.slug` |
| T06 | `emailContact` blank hoặc không đúng email | `400`, `code=4000`, `errors.emailContact` |
| T07 | Slug đã tồn tại, ví dụ `demo-parking-tower` | `409`, `code=4090`, message `Khách hàng đã tồn tại` |
| T08 | JSON sai format | `400`, `code=4001` |
| T09 | Content-Type không phải JSON | `415`, `code=4150` |
| T10 | Manager token tạo tenant | `403`, `code=4030` |

Body mẫu:

```json
{
  "name": "Swagger Tenant 001",
  "slug": "swagger-tenant-001",
  "emailContact": "swagger-tenant-001@example.com"
}
```

### `GET /tenants/{id}`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| TG01 | Không Bearer token | `401`, `code=4010` |
| TG02 | ID tenant vừa tạo hoặc seed tenant active | `200`, trả `TenantResponse` |
| TG03 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| TG04 | ID không đúng UUID format | `400`, thường map về `code=4999` |
| TG05 | Tenant đã suspend | `200`, `status=SUSPENDED` |
| TG06 | Tenant đã soft delete | `404`, do entity có `@SQLRestriction("is_deleted = false")` |
| TG07 | Manager token gọi get tenant | `403`, `code=4030` |

### `PATCH /tenants/{id}/status`

| Case | Request/Setup | Kỳ vọng |
| --- | --- | --- |
| TU01 | Không Bearer token | `401`, `code=4010` |
| TU02 | Admin token, body `{ "status": "SUSPENDED" }` | `200`, trả `TenantResponse.status=SUSPENDED`, evict cache và revoke session active của tenant |
| TU03 | Admin token, body `{ "status": "ACTIVE" }` | `200`, trả `TenantResponse.status=ACTIVE` |
| TU04 | Body thiếu `status` | `400`, `code=4000`, `errors.status` |
| TU05 | `status` không thuộc `ACTIVE/SUSPENDED` | `400`, `code=4001` hoặc `4999` tùy lỗi parse enum |
| TU06 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| TU07 | Manager token update status | `403`, `code=4030` |

### `PATCH /tenants/{id}/suspend`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| TS01 | Không Bearer token | `401`, `code=4010` |
| TS02 | Tenant active tồn tại | `200`, message đình chỉ; gọi GET lại thấy `status=SUSPENDED` |
| TS03 | Gọi suspend lại cùng tenant | `200`, vì service set lại trạng thái, không chặn idempotent |
| TS04 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| TS05 | ID sai UUID format | `400`, thường map về `code=4999` |
| TS06 | Sau khi suspend `demo-parking-tower`, login manager/staff/driver | `403`, `code=4030` |
| TS07 | Manager token suspend tenant | `403`, `code=4030` |

### `DELETE /tenants/{id}`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| TD01 | Không Bearer token | `401`, `code=4010` |
| TD02 | Tenant tồn tại | `200`, soft delete và evict cache |
| TD03 | GET tenant sau TD02 | `404`, `code=4040` |
| TD04 | Delete lại tenant đã soft delete | `404`, do `@SQLRestriction` loại khỏi query |
| TD05 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| TD06 | ID sai UUID format | `400`, thường map về `code=4999` |
| TD07 | Tạo lại tenant cùng slug đã soft delete | Kỳ vọng `409`, do unique constraint DB vẫn giữ slug cũ |
| TD08 | Manager token delete tenant | `403`, `code=4030` |

## 7. Dashboard

### `GET /dashboard/counters`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| D01 | Không Bearer token | `401`, `code=4010` |
| D02 | Admin token | `200`, `activeTenantCount` đếm tenant `ACTIVE` chưa delete, `activeParkingCount` đếm parking `ACTIVE` chưa delete |
| D03 | Manager/staff/driver token | `403`, `code=4030` |

### `GET /dashboard/traffic`

| Case | Request | Kỳ vọng |
| --- | --- | --- |
| TR01 | Admin token, không truyền query | `200`, mặc định lấy 24h gần nhất, `bucket=HOUR` |
| TR02 | Admin token, `bucket=MINUTE/HOUR/DAY` | `200`, group đúng bucket, trả `points[].requestCount/errorCount/averageDurationMs` |
| TR03 | `from >= to` | `400`, `code=4002` |
| TR04 | `bucket` sai enum | `400`, request failed |
| TR05 | Manager/staff/driver token | `403`, `code=4030` |

Ghi chú: filter `ApiTrafficLoggingFilter` ghi traffic của các API nghiệp vụ vào `api_traffic_logs`.
Các path `/dashboard`, Swagger, Actuator và healthcheck được bỏ qua để biểu đồ không tự phình khi mở dashboard.

## 8. Vehicle Types

Lấy ID loại phương tiện bằng SQL:

```sql
select id, name, code, is_active, is_deleted from vehicle_types order by code;
```

### `PATCH /vehicle-types/{id}`

| Case | Request/Setup | Kỳ vọng |
| --- | --- | --- |
| VU01 | Không Bearer token | `401`, `code=4010` |
| VU02 | Admin hoặc manager token, body `{ "name": "Xe máy điện", "code": "E_MOTORBIKE", "active": true }` | `200`, trả `VehicleTypeResponse` mới |
| VU03 | Body chỉ truyền một field, ví dụ `{ "active": false }` | `200`, update partial |
| VU04 | Body rỗng `{}` | `400`, `code=4002` |
| VU05 | `code` trùng loại xe khác | `409`, `code=4090` |
| VU06 | `code` có chữ thường/ký tự lạ | `400`, `code=4000`, `errors.code` |
| VU07 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| VU08 | Staff/driver token | `403`, `code=4030` |

### `DELETE /vehicle-types/{id}`

| Case | Setup | Kỳ vọng |
| --- | --- | --- |
| VD01 | Không Bearer token | `401`, `code=4010` |
| VD02 | Admin hoặc manager token, ID tồn tại | `200`, soft delete: `is_deleted=true`, `is_active=false` |
| VD03 | Delete lại cùng ID | `404`, do entity có `@SQLRestriction("is_deleted = false")` |
| VD04 | UUID hợp lệ nhưng không tồn tại | `404`, `code=4040` |
| VD05 | Staff/driver token | `403`, `code=4030` |

## 9. Checklist chạy end-to-end đề xuất

1. Reset DB bằng SQL ở mục 2.
2. A01, M03: login admin, authorize, kiểm tra `/auth/me`.
3. D02, TR01: kiểm tra counters và traffic dashboard.
4. T02, TG02, TU02, TG05, TU03: tạo tenant mới, get, suspend bằng status API, get lại, active lại.
5. TD02, TD03, TD07: delete tenant vừa tạo và thử tạo lại cùng slug.
6. VU02, VU03, VD02: update partial và delete mềm loại phương tiện.
7. A02, M04: login manager/staff/driver và kiểm tra role/permission.
8. F01, M07: admin force logout một user khác.
9. LA02, LA03: logout-all cover revoke nhiều session.
10. R01, R05: refresh thành công rồi replay refresh cũ.
11. A03-A12, R02-R09, M01-M02, L01-L04, F02-F05, T03-T10, TG03-TG07, TU04-TU07,
    TS03-TS07, TD04-TD08, D03, TR03-TR05, VU04-VU08, VD03-VD05: chạy negative/idempotent cases còn lại.

Khi test xong, reset DB lại nếu cần tiếp tục phát triển local.
