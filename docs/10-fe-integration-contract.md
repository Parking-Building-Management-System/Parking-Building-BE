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
