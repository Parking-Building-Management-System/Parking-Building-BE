# SYSTEM_ADMIN Frontend API Specification

This document is the frontend contract for the SYSTEM_ADMIN portal.

Canonical base path:

```text
/admin
```

Frontend code should use the unversioned `/admin/...` paths. Controllers do not expose a versioned
prefix.

## Common Requirements

### Authentication

All endpoints require:

```http
Authorization: Bearer <access_token>
```

The authenticated user must have:

```text
ROLE_SYSTEM_ADMIN
```

`X-Tenant-Id` is not required for these endpoints.

### Response Envelope

All successful responses use:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {},
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/..."
}
```

### Common Error Envelope

```json
{
  "code": 4000,
  "message": "Validation failed",
  "result": null,
  "errors": {
    "field": "message"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/..."
}
```

Common errors:

| HTTP | Code | Meaning |
| --- | ---: | --- |
| 400 | `4000` | Validation failed |
| 400 | `4001` | Malformed JSON request |
| 401 | `4010` | Missing, expired, invalid token, or revoked session |
| 403 | `4030` | Authenticated user is not `SYSTEM_ADMIN` |
| 404 | `4040` | Resource not found |
| 409 | `4090` | Duplicate resource / database constraint violation |
| 500 | `5000` | Unexpected server error |

## 1. Dashboard Stats

### Get Dashboard Stats

```http
GET /admin/dashboard/stats
```

Headers:

```http
Authorization: Bearer <access_token>
```

Query params:

None.

Request body:

None.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "activeTenantCount": 2,
    "parkingCount": 4,
    "traffic": [
      {
        "bucketStart": "2026-05-14T00:00:00",
        "requestCount": 1200,
        "errorCount": 18,
        "averageDurationMs": 42.5
      },
      {
        "bucketStart": "2026-05-15T00:00:00",
        "requestCount": 1320,
        "errorCount": 12,
        "averageDurationMs": 39.2
      }
    ]
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/dashboard/stats"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 500 | `5000` | Unexpected aggregation/cache/database error |

Frontend cache hint:

- Backend caches this response for 10 minutes under `smartpark:admin:dashboard:stats`.
- TanStack Query can use a matching `staleTime` up to 10 minutes if desired.

## 2. Tenant Management

### List Tenants

```http
GET /admin/tenants
```

Headers:

```http
Authorization: Bearer <access_token>
```

Query params:

| Name | Type | Required | Default | Validation |
| --- | --- | --- | --- | --- |
| `page` | number | No | `0` | `>= 0` |
| `size` | number | No | `20` | `1..100` |

Example:

```http
GET /admin/tenants?page=0&size=20
```

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "content": [
      {
        "id": "4a6f2b7e-4a7b-49a5-8f23-26a78d7f0a01",
        "name": "Vincom Parking",
        "slug": "vincom-parking",
        "emailContact": "manager@vincom.vn",
        "status": "ACTIVE",
        "createdAt": "2026-05-20T09:00:00"
      },
      {
        "id": "722cf3a5-179c-40d8-a9cf-ec8d833f86de",
        "name": "FPT Campus Parking",
        "slug": "fpt-campus-parking",
        "emailContact": "manager@fpt.edu.vn",
        "status": "SUSPENDED",
        "createdAt": "2026-05-19T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/tenants"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4000` | Invalid `page` or `size` |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |

### Provision Tenant

```http
POST /admin/tenants
```

Headers:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Request body:

```json
{
  "companyName": "Vincom Parking",
  "managerEmail": "manager@vincom.vn",
  "initialPassword": "Password@123"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `companyName` | required, non-blank, max 255 chars |
| `managerEmail` | required, valid email, max 255 chars |
| `initialPassword` | required, 8 to 72 chars |

Success response:

```json
{
  "code": 1000,
  "message": "Tạo tenant thành công",
  "result": {
    "id": "4a6f2b7e-4a7b-49a5-8f23-26a78d7f0a01",
    "name": "Vincom Parking",
    "slug": "vincom-parking",
    "emailContact": "manager@vincom.vn",
    "status": "ACTIVE",
    "createdAt": "2026-05-20T10:00:00"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/tenants"
}
```

Side effects:

- Creates a tenant.
- Creates an initial manager user under that tenant.
- Assigns the `PARKING_MANAGER` role to that user.
- Evicts dashboard stats cache.

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4000` | Validation failed |
| 400 | `4001` | Malformed JSON |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 404 | `4040` | Required `PARKING_MANAGER` role is missing |
| 409 | `4090` | Manager email already exists or unique constraint conflict |

### Toggle Tenant Status

```http
PATCH /admin/tenants/{id}/status
```

Headers:

```http
Authorization: Bearer <access_token>
```

Path params:

| Name | Type | Required |
| --- | --- | --- |
| `id` | UUID | Yes |

Request body:

None.

Behavior:

- If current status is `ACTIVE`, backend changes it to `SUSPENDED`.
- If current status is `SUSPENDED`, backend changes it to `ACTIVE`.
- When changed to `SUSPENDED`, all active sessions under the tenant are revoked immediately.

Success response:

```json
{
  "code": 1000,
  "message": "Cập nhật trạng thái tenant thành công",
  "result": {
    "id": "4a6f2b7e-4a7b-49a5-8f23-26a78d7f0a01",
    "status": "SUSPENDED"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/tenants/4a6f2b7e-4a7b-49a5-8f23-26a78d7f0a01/status"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4999` | Invalid UUID path format may be mapped by Spring as request failed |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 404 | `4040` | Tenant not found |
| 500 | `5000` | Unexpected database/session revocation error |

Frontend mutation hint:

- Invalidate tenant list queries after success.
- Invalidate dashboard stats queries after success.

## 3. Master Data: Vehicle Types

### List Vehicle Types

```http
GET /admin/master-data/vehicle-types
```

Headers:

```http
Authorization: Bearer <access_token>
```

Query params:

None.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "5c7e38db-4a55-4a15-94c0-a346cd2c0911",
      "name": "Car 4-seats",
      "code": "CAR_4",
      "active": true,
      "createdAt": "2026-05-20T09:00:00"
    },
    {
      "id": "05e05f90-eaf8-4076-9ab8-f3b8b3f0f2ad",
      "name": "Motorcycle",
      "code": "MOTORCYCLE",
      "active": true,
      "createdAt": "2026-05-20T09:05:00"
    }
  ],
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/master-data/vehicle-types"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 500 | `5000` | Unexpected database/cache error |

Frontend cache hint:

- Backend caches this list under `smartpark:admin:master-data:vehicle-types` without TTL.
- Mutations evict the backend cache.
- Invalidate the TanStack Query key after create/update/delete.

### Create Vehicle Type

```http
POST /admin/master-data/vehicle-types
```

Headers:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Request body:

```json
{
  "name": "Car 4-seats",
  "code": "CAR_4",
  "active": true
}
```

Validation:

| Field | Rules |
| --- | --- |
| `name` | required, non-blank, max 100 chars |
| `code` | required, non-blank, max 50 chars |
| `active` | optional boolean; defaults to `true` when omitted |

Success response:

```json
{
  "code": 1000,
  "message": "Tạo loại phương tiện thành công",
  "result": {
    "id": "5c7e38db-4a55-4a15-94c0-a346cd2c0911",
    "name": "Car 4-seats",
    "code": "CAR_4",
    "active": true,
    "createdAt": "2026-05-20T10:00:00"
  },
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/master-data/vehicle-types"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4000` | Validation failed |
| 400 | `4001` | Malformed JSON |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 409 | `4090` | Vehicle type code already exists |

### Update Vehicle Type

```http
PUT /admin/master-data/vehicle-types/{id}
```

Headers:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Path params:

| Name | Type | Required |
| --- | --- | --- |
| `id` | UUID | Yes |

Request body:

```json
{
  "name": "Car 7-seats",
  "code": "CAR_7",
  "active": true
}
```

Validation:

| Field | Rules |
| --- | --- |
| `name` | required, non-blank, max 100 chars |
| `code` | required, non-blank, max 50 chars |
| `active` | optional boolean |

Success response:

```json
{
  "code": 1000,
  "message": "Cập nhật loại phương tiện thành công",
  "result": {
    "id": "5c7e38db-4a55-4a15-94c0-a346cd2c0911",
    "name": "Car 7-seats",
    "code": "CAR_7",
    "active": true,
    "createdAt": "2026-05-20T10:00:00"
  },
  "timestamp": "2026-05-20T10:05:00Z",
  "path": "/admin/master-data/vehicle-types/5c7e38db-4a55-4a15-94c0-a346cd2c0911"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4000` | Validation failed |
| 400 | `4001` | Malformed JSON |
| 400 | `4999` | Invalid UUID path format may be mapped by Spring as request failed |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 404 | `4040` | Vehicle type not found |
| 409 | `4090` | Vehicle type code already exists |

### Delete Vehicle Type

```http
DELETE /admin/master-data/vehicle-types/{id}
```

Headers:

```http
Authorization: Bearer <access_token>
```

Path params:

| Name | Type | Required |
| --- | --- | --- |
| `id` | UUID | Yes |

Request body:

None.

Behavior:

- Soft delete only.
- Backend sets:
  - `active = false`
  - `deleted = true`
- Backend evicts `smartpark:admin:master-data:vehicle-types`.

Success response:

```json
{
  "code": 1000,
  "message": "Xóa loại phương tiện thành công",
  "timestamp": "2026-05-20T10:10:00Z",
  "path": "/admin/master-data/vehicle-types/5c7e38db-4a55-4a15-94c0-a346cd2c0911"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 400 | `4999` | Invalid UUID path format may be mapped by Spring as request failed |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 404 | `4040` | Vehicle type not found |

## 4. Master Data: Roles

### List Roles

```http
GET /admin/master-data/roles
```

Headers:

```http
Authorization: Bearer <access_token>
```

Query params:

None.

Request body:

None.

Success response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "id": "5072fc60-0c9a-4a41-91c2-e58f3ebc68f1",
      "name": "PARKING_MANAGER",
      "desc": "Parking tenant manager"
    },
    {
      "id": "d2f47e55-7db5-48b7-a0bc-e36964fbbac8",
      "name": "SYSTEM_ADMIN",
      "desc": "System administrator"
    }
  ],
  "timestamp": "2026-05-20T10:00:00Z",
  "path": "/admin/master-data/roles"
}
```

Expected errors:

| HTTP | Code | Case |
| --- | ---: | --- |
| 401 | `4010` | Missing/invalid/revoked token |
| 403 | `4030` | User is not `SYSTEM_ADMIN` |
| 500 | `5000` | Unexpected database error |
