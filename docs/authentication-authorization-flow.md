# Authentication And Authorization Flow

Tài liệu này mô tả các file đang cấu thành feature đăng nhập zero-trust, generate token,
authentication và authorization của SmartPark.

## 1. API Layer

### `AuthenticationController`

File: `src/main/java/com/smartpark/swp391/modules/identity/controller/AuthenticationController.java`

Các endpoint:

- `POST /auth/login`: nhận `AuthenticationRequest`, rate limit theo `username`, gọi
  `AuthenticationService.authenticate`, set `refresh_token` vào HttpOnly cookie và trả về access token.
- `POST /auth/refresh`: nhận refresh token từ cookie `refresh_token` hoặc header `X-Refresh-Token`,
  rotate refresh JTI và trả về token pair mới.
- `GET /auth/me`: đọc access token hiện tại, trả về profile cùng roles và permissions.
- `POST /auth/logout`: revoke session hiện tại và xóa refresh cookie.
- `POST /auth/logout-all`: revoke tất cả session active của user hiện tại.
- `POST /auth/admin/users/{userId}/force-logout`: `SYSTEM_ADMIN` revoke tất cả session của user khác.

DTO liên quan:

- `AuthenticationRequest`: `username`, `password`, `deviceFingerprint`, `deviceLabel`.
- `AuthenticationResponse`: `authenticated`, `accessToken`, `refreshToken`.
- `UserProfileResponse`: profile cơ bản, `tenantId`, roles và permissions.

## 2. Authentication Service

### `AuthenticationServiceImpl`

File: `src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/AuthenticationServiceImpl.java`

Luồng `authenticate`:

1. Tìm user theo `username`.
2. So khớp password bằng `PasswordEncoder` trong `SecurityConfig`.
3. Kiểm tra user và tenant đang `ACTIVE`.
4. Tìm device theo `userId + deviceFingerprint`.
5. Nếu device chưa tồn tại, tạo device `PENDING` và từ chối login bằng `DEVICE_NOT_TRUST`.
6. Nếu device không `APPROVED`, từ chối login.
7. Tạo session mới với `SessionService.createSession`.
8. Tạo `TokenRequest` chứa `userId`, `tenantId`, `sessionId`, subject và `refreshJti`.
9. Gọi `TokenService.generateTokenPair`.

Luồng `refresh`:

1. Verify refresh token và extract claim.
2. Tìm session theo `session_id`.
3. Kiểm tra session chưa revoke, chưa hết hạn và thuộc đúng `user_id` trong token.
4. Rotate `refresh_jti` bằng compare-and-set để chống replay refresh token.
5. Sinh access token mới và refresh token mới với hạn tuyệt đối bằng `session.expiredAt`.

Luồng logout:

- `logout`: revoke đúng session của user hiện tại, ghi revoked marker vào Redis theo TTL access token.
- `logoutAll` và `forceLogout`: revoke toàn bộ session active của user.

## 3. Token Service

### `TokenServiceImpl`

File: `src/main/java/com/smartpark/swp391/modules/identity/service/token/impl/TokenServiceImpl.java`

Token dùng HS512 với secret `jwt.signer-key-base64`.

Access token claims:

- `sub`: username.
- `jti`: UUIDv7 ngẫu nhiên.
- `user_id`: ID user.
- `tenant_id`: ID tenant.
- `session_id`: ID session.
- `iat`, `exp`.

Refresh token claims:

- Tất cả claim trên.
- `typ = REFRESH`.
- `jti = session.refreshJti`.

`verifyRefreshToken` yêu cầu chữ ký hợp lệ, token chưa hết hạn, `typ` đúng `REFRESH` và có `jti`.
Access token không chứa role/permission để tránh stale authorization; quyền được resolve theo session.

## 4. Spring Security

### `SecurityConfig`

File: `src/main/java/com/smartpark/swp391/common/security/config/SecurityConfig.java`

- Public: `/auth/login`, `/auth/refresh`, health check, Swagger và websocket.
- Các request còn lại yêu cầu Bearer JWT.
- `PasswordEncoder`: `BCryptPasswordEncoder(10)`.
- Resource server dùng `CustomJwtDecoder` và `JwtAuthenticationConverter`.
- Authentication/authorization error được trả về bằng format `ApiResponse`.

### `CustomJwtDecoder`

File: `src/main/java/com/smartpark/swp391/common/security/jwt/CustomJwtDecoder.java`

Verify trực tiếp chữ ký HS512 và expiration trước khi để `NimbusJwtDecoder` chuyển token thành
Spring `Jwt`.

### `JwtAuthenticationConverter`

File: `src/main/java/com/smartpark/swp391/common/security/config/JwtAuthenticationConverter.java`

Đọc `user_id`, `session_id`, `exp` từ JWT, gọi `SessionAuthorityResolver` để lấy roles/permissions.
Sau đó map:

- Role DB `SYSTEM_ADMIN` thành authority `ROLE_SYSTEM_ADMIN`.
- Permission DB thành authority `PERM_<permission_name>`.

Vì vậy method security nên dùng:

- `hasRole('SYSTEM_ADMIN')` cho role.
- `hasAuthority('PERM_QUYEN_TREN_APP.BAI_XE.SLOT.VIEW')` cho permission chi tiết.

## 5. Session Guard And Authorization Cache

### `SessionAuthorityResolver`

File: `src/main/java/com/smartpark/swp391/common/security/config/SessionAuthorityResolver.java`

Điểm trung tâm của authorization runtime:

1. Tính Redis TTL theo thời gian hết hạn của JWT.
2. Gọi `SessionGuardService.ensureActive` để đảm bảo session chưa bị revoke.
3. Nếu Redis có authz cache theo session, trả về ngay.
4. Nếu cache miss, gọi `AuthorityLoader.load(userId)`, lưu Redis và trả về.
5. Nếu Redis lỗi, fallback DB để request vẫn có thể chạy.

### `SessionGuardService`

File: `src/main/java/com/smartpark/swp391/common/security/config/SessionGuardService.java`

Kiểm tra 3 lớp:

- Revoked marker trong Redis.
- Active marker trong Redis.
- Fallback DB bằng `SessionRepository.isSessionActive`.

Nếu DB báo session inactive, service ghi revoked marker và xóa cache liên quan.

### `SessionAuthorityCacheService`

File: `src/main/java/com/smartpark/swp391/infrastructure/cached/redis/service/SessionAuthorityCacheService.java`

Quản lý các key Redis:

- Authz cache theo session.
- Active marker theo session.
- Revoked marker theo session.

## 6. Authority Loader And Repositories

### `AuthorityLoaderImpl`

File: `src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/AuthorityLoaderImpl.java`

Load tenant, roles và permissions từ DB:

- `UserRepository.findTenantIdByUserId`
- `RoleRepository.findRoleNamesByUserId`
- `PermissionRepository.findPermissionNamesByUserId`

Kết quả được đóng gói trong `SessionAuthzCache`.

### Entity chính

- `User`: thuộc một tenant, có username/password/status.
- `Device`: device fingerprint của user, trạng thái `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.
- `Session`: session đăng nhập, `refreshJti`, `revokedAt`, `expiredAt`.
- `Role`, `Permission`, `UserRole`, `RolePermission`: RBAC.
- `Tenant`: tenant SaaS, có status và soft delete.

## 7. Seed Data

File: `src/main/resources/db/migration/V20260518093000__seed_identity_auth.sql`

Migration seed theo thứ tự:

1. Permissions theo nghiệp vụ bãi xe.
2. Roles: `SYSTEM_ADMIN`, `PARKING_MANAGER`, `STAFF`, `PARKING_USER`.
3. Role-permission mappings.
4. Tenants mẫu.
5. Users mẫu.
6. User-role mappings.
7. Approved devices mẫu cho zero-trust login.

Tất cả user seed dùng password `Password@123`, hash bằng BCrypt strength 10.

## 8. Security Notes

- Không đưa roles/permissions vào JWT; quyền được resolve từ DB/Redis theo session để có thể thu hồi nhanh.
- Refresh token rotation dùng compare-and-set trên `refresh_jti`; refresh token cũ dùng lại sẽ fail.
- Login device lạ chỉ tạo device `PENDING`, không cấp token.
- Endpoint protected phải đi qua resource server. Nếu thêm base path mới, cần đảm bảo `SecurityConfig`
  vẫn match path đó.
- Role name trong DB nên giữ dạng uppercase snake case để tương thích `hasRole`.
