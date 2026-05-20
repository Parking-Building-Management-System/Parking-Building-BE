# 04. Database, Entities, Migrations

## Database đang dùng

Backend dùng PostgreSQL. Khai báo ở:

- `pom.xml`: dependency `org.postgresql:postgresql`.
- `src/main/resources/application.yaml`: `driver-class-name: org.postgresql.Driver`, `database-platform: org.hibernate.dialect.PostgreSQLDialect`.

Hibernate đang `ddl-auto: validate`, nên DB schema phải được tạo bằng migration, không tự tạo bảng từ entity.

## Entity package

Entity nằm ở `src/main/java/com/smartpark/swp391/modules/identity/entity`.

Base entity nằm ở `src/main/java/com/smartpark/swp391/infrastructure/persistence/BaseEntity.java`.

`BaseEntity` cung cấp:

- `id` UUID, sinh bằng custom UUIDv7 generator.
- `createdAt`, map cột `created_at`.
- `updatedAt`, map cột `updated_at`.
- `equals/hashCode` tránh lỗi với Hibernate proxy.

## Entity chính

### Tenant

File: `modules/identity/entity/Tenant.java`

Table: `tenants`

Field chính:

- `name`
- `slug`, unique
- `emailContact`, cột `email_contact`
- `status`: `TenantStatus`
- `isDeleted`, cột `is_deleted`

Relation: chưa thấy relation trực tiếp trong `Tenant`, nhưng `User` many-to-one tới `Tenant`.

Status enum: `TenantStatus.ACTIVE`, `TenantStatus.SUSPENDED`.

Ghi chú: entity có `@SQLRestriction("is_deleted = false")`, nên query JPA mặc định bỏ qua tenant đã soft delete.

### User

File: `modules/identity/entity/User.java`

Table: `users`

Field chính:

- `tenant`: many-to-one `Tenant`, cột `tenant_id`.
- `username`, unique.
- `password`, BCrypt hash.
- `fullName`, cột `full_name`.
- `phone`.
- `status`: `UserStatus`.
- `createdBy`, cột `created_by`.
- `isDeleted`, cột `is_deleted`.

Status enum: `ACTIVE`, `INACTIVE`, `SUSPENDED`.

Ghi chú: có `@SQLRestriction("is_deleted = false")`.

### Role

File: `modules/identity/entity/Role.java`

Table: `roles`

Field chính:

- `name`, unique.
- `desc`, map mặc định tới cột tên `desc`. Migration tạo cột `"desc"` vì đây có thể là keyword nhạy cảm trong SQL.

Seed role: `SYSTEM_ADMIN`, `PARKING_MANAGER`, `STAFF`, `PARKING_USER`.

### Permission

File: `modules/identity/entity/Permission.java`

Table: `permissions`

Field chính:

- `name`, unique.
- `scope`.
- `module`.
- `resource`.
- `label`.
- `action`.

Permission seed có dạng `QUYEN_TREN_APP.HE_THONG.TENANT.CREATE`, `QUYEN_TREN_APP.BAI_XE.SLOT.VIEW`, ...

### UserRole

File: `modules/identity/entity/UserRole.java`

Table: `user_roles`

Relation:

- many-to-one `User`.
- many-to-one `Role`.

Constraint:

- unique `(user_id, role_id)`.
- FK có `ON DELETE CASCADE` trong migration.

### RolePermission

File: `modules/identity/entity/RolePermission.java`

Table: `role_permissions`

Relation:

- many-to-one `Role`.
- many-to-one `Permission`.

Constraint:

- unique `(role_id, permission_id)`.
- FK có `ON DELETE CASCADE` trong migration.

### Device

File: `modules/identity/entity/Device.java`

Table: `devices`

Field chính:

- `user`: many-to-one `User`, cột `user_id`.
- `fingerprint`.
- `label`.
- `status`: `DeviceStatus`.
- `approvedBy`.
- `approvedAt`.
- `expiresAt`.

Status enum: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.

Constraint:

- unique `(user_id, fingerprint)`.

### Session

File: `modules/identity/entity/Session.java`

Table: `sessions`

Field chính:

- `user`: many-to-one `User`.
- `device`: many-to-one `Device`.
- `refreshJti`, unique, cột `refresh_jti`.
- `revokedAt`, cột `revoked_at`.
- `expiredAt`, cột `expired_at`.

Session active khi:

- `revokedAt IS NULL`
- `expiredAt > now`

## Entity chưa thấy trong code hiện tại

Chưa thấy các entity sau trong repo hiện tại:

- `Vehicle`
- `VehicleCategory`
- `Parking`
- `ParkingLot`
- `ParkingBuilding`
- `ParkingSlot`
- `Dashboard`
- `Traffic`

Các task Sprint 1 liên quan các entity này cần confirm schema/API contract với FE/leader trước khi code. Không được bịa field database.

## Migration

Flyway config:

- `src/main/resources/application.yaml`
- `spring.flyway.enabled: true`
- `locations: classpath:db/migration`
- `baseline-on-migrate: true`
- `out-of-order: true`

Migration nằm ở `src/main/resources/db/migration`.

File hiện có:

- `V20260515023500__init_tenant_table.sql`
- `V20260516162100__init_identity_tables.sql`
- `V20260518093000__seed_identity_auth.sql`

Naming convention hiện tại:

```txt
VyyyyMMddHHmmss__short_description.sql
```

Không sửa migration cũ đã chạy. Nếu đổi schema, tạo file mới có timestamp lớn hơn.

## Seed data

Seed tenant:

- Trong `V20260518093000__seed_identity_auth.sql`.
- Tenant `SmartPark SaaS`, slug `smartpark-saas`.
- Tenant `Demo Parking Tower`, slug `demo-parking-tower`.

Seed user:

- `system.admin@smartpark.local`
- `manager@demo-parking.local`
- `staff@demo-parking.local`
- `driver@demo-parking.local`
- Password mặc định ghi trong comment migration: `Password@123`.

Seed device:

- `seed-system-admin-device`
- `seed-manager-device`
- `seed-staff-device`
- `seed-driver-device`
- Tất cả status `APPROVED`.

Seed role/permission:

- Permissions được insert trước.
- Roles được insert sau.
- `role_permissions` map theo role.
- `user_roles` map user với role.

## Common errors

Entity đổi nhưng DB không đổi:

- Dấu hiệu: app fail start với Hibernate validate.
- Sửa: tạo migration mới.

Migration chạy sai thứ tự:

- Dấu hiệu: table/constraint referenced chưa tồn tại.
- Kiểm tra timestamp version.

Nullable mismatch:

- Dấu hiệu: DB cho null nhưng entity `nullable=false` hoặc ngược lại.
- Kiểm tra entity annotation và migration.

Enum string mismatch:

- Entity dùng `EnumType.STRING`.
- DB phải chứa đúng text enum Java, ví dụ `ACTIVE`, `SUSPENDED`.

Lazy loading lỗi:

- Không trả entity trực tiếp ra API.
- Map sang DTO trong service khi còn transaction hoặc query đủ dữ liệu.

Duplicate key:

- `tenants.slug`, `users.username`, `roles.name`, `permissions.name`, `(user_id, role_id)`, `(role_id, permission_id)`, `(user_id, fingerprint)`, `sessions.refresh_jti`.
- Nên check trước ở service và vẫn handle `DataIntegrityViolationException`.
