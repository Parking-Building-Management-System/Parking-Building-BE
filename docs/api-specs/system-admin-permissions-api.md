# System Admin Permissions API Spec

## Scope

These APIs manage global permission definitions and role permission assignments for SmartPark. They are available to `SYSTEM_ADMIN` only and must not expose tenant-scoped business data.

Permissions are stored as flat rows and can be grouped into a tree:

```text
scope -> module -> resource -> label -> actions
```

## Permission Row

```json
{
  "id": "uuid",
  "scope": "PARKING_MANAGER",
  "module": "FACILITY",
  "resource": "SLOT",
  "label": "Slot Management",
  "action": "VIEW"
}
```

## Tree Response

```json
[
  {
    "scope": "PARKING_MANAGER",
    "modules": [
      {
        "module": "FACILITY",
        "resources": [
          {
            "resource": "SLOT",
            "labels": [
              {
                "label": "Slot Management",
                "actions": [
                  {
                    "action": "VIEW",
                    "id": "uuid"
                  },
                  {
                    "action": "CREATE",
                    "id": "uuid"
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
]
```

## Endpoints

### GET /admin/permissions/tree

Purpose: return all active permission definitions grouped as a permission tree.

Response:

```json
{
  "code": 1000,
  "message": "Success",
  "result": []
}
```

Notes:

- Cacheable.
- Used by System Admin Master Data UI.
- Deterministic ordering should be applied by `scope`, `module`, `resource`, `label`, then `action`.

### GET /admin/roles

Purpose: list global roles.

Response item:

```json
{
  "id": "uuid",
  "name": "PARKING_MANAGER",
  "description": "Tenant parking manager"
}
```

### GET /admin/roles/{roleId}/permissions

Purpose: return the permission tree for one role with selected action ids.

Response action item:

```json
{
  "id": "uuid",
  "action": "VIEW",
  "selected": true
}
```

### PUT /admin/roles/{roleId}/permissions

Purpose: replace role permission assignments.

Request:

```json
{
  "permissionIds": ["uuid", "uuid"]
}
```

Response:

```json
{
  "roleId": "uuid",
  "permissionCount": 2
}
```

### POST /admin/permissions

Purpose: create a permission definition.

Request:

```json
{
  "scope": "PARKING_MANAGER",
  "module": "FACILITY",
  "resource": "SLOT",
  "label": "Slot Management",
  "action": "VIEW"
}
```

### PUT /admin/permissions/{id}

Purpose: update permission metadata such as `label`, `description`, or `status`.

### DELETE /admin/permissions/{id}

Purpose: soft delete a permission when safe. Backend should reject deletion when the permission is still assigned to roles unless a deliberate force option is introduced later.

## Backend Implementation Notes

- Query flat rows from the `permissions` table.
- Build the tree in O(n) using LinkedHashMap-like grouping to preserve deterministic order.
- Add cache for the all-permission tree.
- Add cache per role permission tree.
- Invalidate all permission tree cache on permission create, update, delete.
- Invalidate role permission cache on role permission update.
- Do not expose tenant-scoped data here; permissions are global master data.
- Restrict all endpoints to `SYSTEM_ADMIN`.

## Recommended DB Fields

`permissions`:

- `id UUID`
- `name/code` optional
- `scope`
- `module`
- `resource`
- `label`
- `action`
- `description` optional
- `status`
- `created_at`
- `updated_at`
- `is_deleted`

`role_permissions`:

- `role_id`
- `permission_id`

