# 10. FE Integration Contract

Tài liệu này dành cho backend khi viết API để FE tích hợp dễ và ít lệch contract.

Nguyên tắc quan trọng:

- Không đổi response field tùy tiện sau khi FE đã tích hợp.
- Nếu đổi DTO/request/response phải báo FE.
- Với enum/status phải gửi danh sách giá trị rõ ràng.
- Với date/time phải thống nhất ISO string.
- Không trả entity trực tiếp.
- Mọi API nên dùng `ApiResponse`.
- Error response phải dùng `ErrorCode` hiện có hoặc code mới đã thống nhất.

## Mỗi API mới bắt buộc document

- Method.
- URL.
- Auth required hay không.
- Role/permission required.
- Request body example.
- Query params example.
- Success response example.
- Error response example.
- Error code/message.
- Pagination format nếu có.
- Sort/filter format nếu có.
- Field meaning.
- Nullable field.
- Enum values.
- Cache behavior nếu có.
- Notes cho FE.

## API Contract Template

Copy template này khi viết docs cho API mới.

````md
## API Name

### Method + URL

`GET /example-resources`

### Auth

Required: yes/no

Roles:

- `SYSTEM_ADMIN`

Permissions:

- `PERM_QUYEN_TREN_APP.HE_THONG.EXAMPLE.VIEW`

### Request

Request body:

```json
{
  "name": "Example",
  "status": "ACTIVE"
}
```

Field meaning:

| Field | Type | Required | Nullable | Meaning |
| --- | --- | --- | --- | --- |
| `name` | string | yes | no | Tên hiển thị |
| `status` | enum | yes | no | Trạng thái |

Enum values:

- `ACTIVE`
- `SUSPENDED`

### Query Params

Example:

```txt
?page=0&size=20&sort=createdAt,desc&keyword=demo&status=ACTIVE
```

| Param | Type | Required | Default | Meaning |
| --- | --- | --- | --- | --- |
| `page` | number | no | `0` | Page index, 0-based |
| `size` | number | no | `20` | Page size |
| `sort` | string | no | `createdAt,desc` | Sort field and direction |
| `keyword` | string | no | none | Filter keyword |
| `status` | enum | no | none | Filter status |

### Success Response

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  },
  "timestamp": "2026-05-18T00:00:00Z",
  "path": "/example-resources"
}
```

### Error Responses

Validation error:

```json
{
  "code": 4000,
  "message": "Validation failed",
  "errors": {
    "name": "Name must not be blank"
  },
  "timestamp": "2026-05-18T00:00:00Z",
  "path": "/example-resources"
}
```

Unauthorized:

```json
{
  "code": 4010,
  "message": "Unauthenticated",
  "timestamp": "2026-05-18T00:00:00Z",
  "path": "/example-resources"
}
```

Forbidden:

```json
{
  "code": 4030,
  "message": "Forbidden",
  "timestamp": "2026-05-18T00:00:00Z",
  "path": "/example-resources"
}
```

### Cache Behavior

- Cache key:
- TTL:
- Khi nào cache bị invalidate:
- FE có cần tự refetch sau create/update/delete không:

### Notes for Frontend

- Page index là 0-based.
- Date/time dùng ISO string.
- Không hardcode message để xử lý logic; hãy dùng `code`.
- Nếu API dùng cookie refresh token, request phải bật credentials.
````

## Contract cho Auth hiện tại

### POST /auth/login

Auth required: no.

Request:

```json
{
  "username": "system.admin@smartpark.local",
  "password": "Password@123",
  "deviceFingerprint": "seed-system-admin-device",
  "deviceLabel": "Admin laptop"
}
```

Success result:

```json
{
  "authenticated": true,
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>"
}
```

Backend cũng set cookie:

```txt
Set-Cookie: refresh_token=<jwt>; HttpOnly; Secure; SameSite=None; Path=/
```

Notes cho FE:

- Dùng `accessToken` cho Bearer API.
- Không lưu refresh token vào localStorage.
- Nếu dùng cookie refresh, bật credentials.
- Device lạ sẽ fail và backend tạo device `PENDING`.

### POST /auth/refresh

Auth required: no.

Refresh token lấy từ:

- Cookie `refresh_token`, hoặc
- Header `X-Refresh-Token`.

Success trả access token mới và set cookie refresh token mới.

### GET /auth/me

Auth required: yes.

Header:

```txt
Authorization: Bearer <accessToken>
```

Success result field:

- `id`
- `tenantId`
- `username`
- `fullName`
- `phone`
- `roles`
- `permissions`

### POST /auth/logout

Auth required: yes.

Effect:

- Revoke session hiện tại.
- Clear refresh cookie.
- Access token cũ không nên dùng được nữa.

## Contract cho Tenant hiện tại

### POST /tenants

Auth required: yes theo `SecurityConfig`.

Permission: cần confirm. Code hiện tại chưa có `@PreAuthorize`, dù Swagger tag ghi dành cho System Admin.

Request:

```json
{
  "name": "Demo Tenant",
  "slug": "demo-tenant",
  "emailContact": "owner@demo.local"
}
```

Validation:

- `name`: not blank.
- `slug`: not blank, chỉ chữ thường/số/gạch ngang theo regex `^[a-z0-9-]+$`.
- `emailContact`: not blank, email.

Success result:

```json
{
  "id": "uuid",
  "name": "Demo Tenant",
  "slug": "demo-tenant",
  "emailContact": "owner@demo.local",
  "status": "ACTIVE",
  "createdAt": "2026-05-18T00:00:00"
}
```

### GET /tenants/{id}

Auth required: yes.

Permission: cần confirm.

Success result là `TenantResponse`.

Cache behavior:

- Cache Redis key `smartpark:tenant:detail:<tenantId>`.
- TTL 1 giờ.

### PATCH /tenants/{id}/suspend

Auth required: yes.

Permission: cần confirm.

Effect:

- Set tenant status `SUSPENDED`.
- Evict tenant cache.

Ghi chú cho FE:

- Code hiện tại chưa thấy revoke toàn bộ session user thuộc tenant khi suspend. Cần kiểm tra lại trong code hiện tại nếu FE kỳ vọng user bị kick ngay.

### DELETE /tenants/{id}

Auth required: yes.

Permission: cần confirm.

Effect:

- Soft delete bằng `is_deleted = true`.
- Evict tenant cache.

Ghi chú cho FE:

- Sau delete, JPA query bị `@SQLRestriction("is_deleted = false")` nên tenant không còn hiện trong read API.

## Contract cho PARKING_MANAGER Facility APIs

Auth required: yes.

Required role:

- `PARKING_MANAGER`

Tenant isolation:

- FE does not send `X-Tenant-Id`.
- Backend reads `tenant_id` from the authenticated JWT and sets `TenantContext`.
- Hibernate tenant filter applies to parking, floor, zone, and slot queries.

Base paths:

- `/manager/parkings`
- `/manager/floors`
- `/manager/zones`
- `/manager/slots`

### Parkings

`GET /manager/parkings`

Success result:

```json
[
  {
    "id": "uuid",
    "code": "VINCOM-DK",
    "name": "Vincom Dong Khoi",
    "address": "72 Le Thanh Ton, District 1, Ho Chi Minh City",
    "totalCapacity": 120,
    "status": "ACTIVE"
  }
]
```

Notes:

- `totalCapacity` is calculated from current slot count, not trusted from stale FE state.

`PATCH /manager/parkings/{id}/status`

Behavior:

- `ACTIVE -> MAINTENANCE`
- any non-active parking status currently toggles back to `ACTIVE`

Success result:

```json
{
  "id": "uuid",
  "status": "MAINTENANCE"
}
```

`GET /manager/parkings/{id}/topology`

Success result:

```json
{
  "id": "uuid",
  "code": "VINCOM-DK",
  "name": "Vincom Dong Khoi",
  "status": "ACTIVE",
  "totalCapacity": 120,
  "floors": [
    {
      "id": "uuid",
      "code": "B1",
      "name": "Basement 1",
      "displayOrder": 1,
      "zones": [
        {
          "id": "uuid",
          "code": "CAR-A",
          "name": "Car Area A",
          "vehicleTypeCode": "CAR",
          "vehicleTypeName": "Car",
          "capacity": 60,
          "slotCount": 60,
          "status": "ACTIVE"
        }
      ]
    }
  ]
}
```

Cache behavior:

- Cache key: `smartpark:tenant:{tenantId}:topology:{parkingId}`
- TTL: 10 minutes
- Backend evicts when parking status, floors, zones, or slots mutate.
- FE should invalidate/refetch topology after any facility or slot mutation succeeds.

### Floors

Endpoints:

| Method | URL | Meaning |
| --- | --- | --- |
| `GET` | `/manager/parkings/{parkingId}/floors` | List floors in a parking |
| `POST` | `/manager/parkings/{parkingId}/floors` | Create floor |
| `GET` | `/manager/floors/{id}` | Get floor detail |
| `PUT` | `/manager/floors/{id}` | Update floor |
| `DELETE` | `/manager/floors/{id}` | Soft-delete an empty floor |

Create/update request:

```json
{
  "code": "B1",
  "name": "Basement 1",
  "displayOrder": 1,
  "active": true
}
```

Success result:

```json
{
  "id": "uuid",
  "parkingId": "uuid",
  "code": "B1",
  "name": "Basement 1",
  "displayOrder": 1,
  "active": true
}
```

Validation:

- `code`: required, max 50, unique inside the parking.
- `name`: required, max 100.
- `displayOrder`: required.
- Delete fails if the floor still has zones.

### Zones

Endpoints:

| Method | URL | Meaning |
| --- | --- | --- |
| `GET` | `/manager/floors/{floorId}/zones` | List zones in a floor |
| `POST` | `/manager/floors/{floorId}/zones` | Create zone |
| `GET` | `/manager/zones/{id}` | Get zone detail |
| `PUT` | `/manager/zones/{id}` | Update zone |
| `DELETE` | `/manager/zones/{id}` | Soft-delete an empty zone |

Create/update request:

```json
{
  "code": "CAR-A",
  "name": "Car Area A",
  "vehicleTypeCode": "CAR",
  "capacity": 60,
  "status": "ACTIVE"
}
```

Success result:

```json
{
  "id": "uuid",
  "parkingId": "uuid",
  "floorId": "uuid",
  "code": "CAR-A",
  "name": "Car Area A",
  "vehicleTypeCode": "CAR",
  "vehicleTypeName": "Car",
  "capacity": 60,
  "status": "ACTIVE"
}
```

Validation:

- `vehicleTypeCode` must exist in global `vehicle_types` and be active.
- Zone code must be unique inside the parking.
- Delete fails if the zone still has slots.

Enum values:

- `ZoneStatus`: `ACTIVE`, `INACTIVE`, `MAINTENANCE`

### Slots

`GET /manager/slots`

Query params:

| Param | Type | Required | Default | Meaning |
| --- | --- | --- | --- | --- |
| `zoneId` | UUID | no | none | Filter by zone |
| `status` | enum | no | none | Filter by slot status |
| `slotCode` | string | no | none | Filter by slot code |
| `exact` | boolean | no | `false` | Exact match when true, LIKE search when false |
| `page` | number | no | `0` | 0-based page |
| `size` | number | no | `20` | 1..100 |

Success result uses `PageResponse`:

```json
{
  "content": [
    {
      "id": "uuid",
      "parkingId": "uuid",
      "parkingName": "Vincom Dong Khoi",
      "floorId": "uuid",
      "floorName": "Basement 1",
      "zoneId": "uuid",
      "zoneName": "Car Area A",
      "code": "A01",
      "slotNumber": "A01",
      "status": "AVAILABLE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`PATCH /manager/slots/bulk-status`

Request:

```json
{
  "slotIds": ["uuid-1", "uuid-2"],
  "newStatus": "MAINTENANCE"
}
```

Allowed `newStatus` values:

- `AVAILABLE`
- `MAINTENANCE`
- `LOCKED`

Success result:

```json
{
  "updatedCount": 2,
  "newStatus": "MAINTENANCE"
}
```

`POST /manager/slots/import`

Content type: `multipart/form-data`

Form field:

- `file`: Excel workbook

Required Excel headers:

- `parkingCode`
- `floorCode`
- `zoneCode`
- `slotCode`

Optional Excel headers:

- `slotNumber`
- `status`

Rules:

- Parking, floor, and zone must exist under the JWT tenant.
- Slot code must be unique inside the zone.
- Blank rows are ignored.
- If any row is invalid, backend rejects the import instead of partially inserting.

Success result:

```json
{
  "insertedCount": 25
}
```

`GET /manager/slots/export`

Response:

- Content-Type: `application/vnd.ms-excel`
- Content-Disposition: attachment
- Body: Excel file with all tenant slots.

Slot status enum values:

- `AVAILABLE`
- `OCCUPIED`
- `RESERVED`
- `MAINTENANCE`
- `LOCKED`

Common expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4000` | Validation failed |
| 400 | `4002` | Invalid import file, invalid status, inactive vehicle type, or delete blocked by children |
| 401 | `4010` | Missing/invalid/revoked token or missing tenant claim |
| 403 | `4030` | User is not `PARKING_MANAGER` |
| 404 | `4040` | Parking, floor, zone, vehicle type, or slot not found inside tenant |
| 409 | `4090` | Duplicate floor, zone, or slot code |

## Error code nên FE biết

| Code | Meaning |
| --- | --- |
| `1000` | Success |
| `4000` | Validation failed |
| `4001` | Malformed JSON |
| `4003` | Sai thông tin đăng nhập |
| `4005` | Thiết bị chưa được tin cậy |
| `4010` | Unauthenticated |
| `4030` | Forbidden |
| `4040` | Not found |
| `4090` | Conflict |
| `4290` | Too many requests |
| `5000` | Unexpected error |

FE nên branch logic theo `code`, không branch theo message tiếng Việt.
