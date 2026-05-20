# 03. Auth, Security, Session

## File chính cần đọc

- `src/main/java/com/smartpark/swp391/modules/identity/controller/AuthenticationController.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/auth/AuthenticationService.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/AuthenticationServiceImpl.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/auth/SessionService.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/auth/impl/SessionServiceImpl.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/token/TokenService.java`
- `src/main/java/com/smartpark/swp391/modules/identity/service/token/impl/TokenServiceImpl.java`
- `src/main/java/com/smartpark/swp391/common/security/config/SecurityConfig.java`
- `src/main/java/com/smartpark/swp391/common/security/jwt/CustomJwtDecoder.java`
- `src/main/java/com/smartpark/swp391/common/security/config/JwtAuthenticationConverter.java`
- `src/main/java/com/smartpark/swp391/common/security/config/SessionAuthorityResolver.java`
- `src/main/java/com/smartpark/swp391/common/security/config/SessionGuardService.java`
- `src/main/java/com/smartpark/swp391/infrastructure/cached/redis/service/SessionAuthorityCacheService.java`

## AuthenticationController

Controller nằm ở `modules/identity/controller/AuthenticationController.java`, base path `/auth`.

Endpoint hiện có:

- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/me`
- `POST /auth/logout`
- `POST /auth/logout-all`
- `POST /auth/admin/users/{userId}/force-logout`

`force-logout` có `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`. Các endpoint còn lại dựa vào `SecurityConfig`: login/refresh public, các endpoint khác cần Bearer token.

## DTO auth

`AuthenticationRequest` ở `dto/authentication/request/AuthenticationRequest.java` gồm:

- `username`: `@NotBlank`
- `password`: `@NotBlank`
- `deviceFingerprint`: `@NotBlank`
- `deviceLabel`: optional

`AuthenticationResponse` ở `dto/authentication/response/AuthenticationResponse.java` gồm:

- `authenticated`
- `accessToken`
- `refreshToken`

Lưu ý: backend vừa trả refresh token trong response body, vừa set `refresh_token` HttpOnly cookie. Về security, FE không nên lưu refresh token vào localStorage. Cần hỏi leader/backend owner nếu muốn bỏ refresh token khỏi body sau khi FE chuyển sang cookie-only.

`UserProfileResponse` ở `dto/authentication/response/UserProfileResponse.java` gồm:

- `id`
- `tenantId`
- `username`
- `fullName`
- `phone`
- `roles`
- `permissions`

## SecurityConfig

`SecurityConfig` bật:

- `@EnableWebSecurity`
- `@EnableMethodSecurity`
- `PasswordEncoder`: `BCryptPasswordEncoder(10)`
- Public endpoints: `/auth/login`, `/auth/refresh`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, health check.
- Swagger public: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.
- `/ws/**` public.
- Các request còn lại cần authenticated.
- OAuth2 Resource Server dùng `CustomJwtDecoder` và `JwtAuthenticationConverter`.

`JwtAuthenticationConverter` map:

- Role DB `SYSTEM_ADMIN` -> authority `ROLE_SYSTEM_ADMIN`.
- Permission DB `QUYEN_TREN_APP...` -> authority `PERM_QUYEN_TREN_APP...`.

Dùng trong code:

```java
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@PreAuthorize("hasAuthority('PERM_QUYEN_TREN_APP.BAI_XE.SLOT.VIEW')")
```

## Flow login

```txt
1. Client gửi username/password/deviceFingerprint/deviceLabel tới POST /auth/login.
2. AuthenticationController validate @Valid AuthenticationRequest.
3. @RateLimit giới hạn 5 request/60 giây theo #request.username.
4. AuthenticationServiceImpl tìm user bằng UserRepository.findByUsername.
5. Check password bằng PasswordEncoder.matches.
6. Check user.status == ACTIVE và user.tenant.status == ACTIVE.
7. DeviceRepository.findByUserIdAndFingerprint.
8. Nếu device chưa tồn tại:
   - tạo Device status PENDING
   - throw DEVICE_NOT_TRUST
   - không cấp token
9. Nếu device tồn tại nhưng không APPROVED:
   - throw DEVICE_NOT_TRUST
10. SessionServiceImpl.createSession:
   - tạo Session user/device
   - set expiredAt = now + jwt.refreshable-duration
   - set refreshJti = UUIDv7
11. TokenServiceImpl.generateTokenPair:
   - access token
   - refresh token typ=REFRESH, jti=session.refreshJti
12. Controller set refresh_token HttpOnly cookie:
   - httpOnly true
   - secure theo cookie.secure, default true
   - SameSite=None
   - path theo cookie.path, default /
13. Trả ApiResponse<AuthenticationResponse>.
```

## Flow refresh

```txt
1. Client gọi POST /auth/refresh.
2. Backend lấy refresh token từ cookie refresh_token hoặc header X-Refresh-Token.
3. AuthenticationServiceImpl.refresh gọi TokenServiceImpl.extractToken.
4. TokenServiceImpl verify:
   - chữ ký HS512 hợp lệ
   - chưa hết hạn
   - typ == REFRESH
   - có jti
5. Tìm Session theo session_id.
6. Check session chưa revoked, chưa expired.
7. Check session.user.id đúng user_id trong token.
8. Generate newJti.
9. SessionRepository.rotateRefreshJti(sessionId, oldJti, newJti).
   Đây là compare-and-set để chống replay refresh token.
10. Nếu updated != 1 thì token cũ/replay -> UNAUTHENTICATED.
11. Sinh accessToken mới.
12. Sinh refreshToken mới với expiry tuyệt đối theo session.expiredAt.
13. Set cookie refresh_token mới.
14. Trả ApiResponse<AuthenticationResponse>.
```

## Flow /auth/me

```txt
1. Client gửi Bearer access token.
2. CustomJwtDecoder verify chữ ký/expiration.
3. JwtAuthenticationConverter đọc user_id, session_id, exp.
4. SessionAuthorityResolver.resolve:
   - tính TTL theo exp của JWT
   - SessionGuardService.ensureActive
   - đọc Redis authz cache theo session
   - nếu miss: AuthorityLoaderImpl load roles/permissions từ DB và put cache
5. AuthenticationServiceImpl.getMyProfile đọc SecurityContext.
6. Lấy user bằng UserRepository.findById.
7. Trả UserProfileResponse gồm profile, roles, permissions.
```

## Flow logout

`POST /auth/logout`:

- Lấy `session_id`, `user_id` từ JWT.
- `AuthenticationServiceImpl.logout`.
- `SessionServiceImpl.revoke(sessionId, userId, accessTtl)`.
- DB update `sessions.revoked_at`.
- Redis best-effort:
  - mark revoked marker.
  - clear authz cache.
  - clear active marker.
- Clear cookie `refresh_token` bằng maxAge 0.

`POST /auth/logout-all`:

- Lấy `user_id` từ JWT.
- Tìm active sessions của user.
- Update revoke toàn bộ session active.
- Mark revoked/clear cache từng session.
- Clear cookie request hiện tại.

`POST /auth/admin/users/{userId}/force-logout`:

- Chỉ `SYSTEM_ADMIN`.
- Revoke toàn bộ session active của target user.

## Access token vs refresh token

Access token:

- Dùng gọi API protected bằng `Authorization: Bearer <token>`.
- TTL ngắn theo `JWT_VALID_DURATION`.
- Claim có `user_id`, `tenant_id`, `session_id`.
- Không chứa role/permission để tránh stale authorization.

Refresh token:

- Dùng lấy access token mới qua `/auth/refresh`.
- TTL dài hơn theo `JWT_REFRESHABLE_DURATION`, nhưng bị ràng buộc bởi session DB.
- Có `typ=REFRESH`.
- Có `jti=session.refreshJti`.
- Được rotate mỗi lần refresh.

Refresh token không nên lưu localStorage vì JavaScript đọc được localStorage; nếu XSS xảy ra, token bị lộ. HttpOnly cookie làm JavaScript không đọc được token, giảm rủi ro token theft.

## HttpOnly cookie, CORS, credentials

Cookie refresh được set trong `AuthenticationController`:

- Name: `refresh_token`.
- `HttpOnly=true`.
- `SameSite=None`.
- `Secure` lấy từ `cookie.secure`, default `true`.
- `path` lấy từ `cookie.path`, default `/`.

`CorsConfig`:

- Allowed origins từ `app.cors.allowed-origins`.
- Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Headers: `*`.
- `allowCredentials(true)`.

FE cần bật credentials khi muốn browser gửi cookie:

- Axios: `withCredentials: true`.
- Fetch: `credentials: "include"`.

## Trusted device/fingerprint

Device fingerprint dùng để định danh browser/device. Backend mới là nơi quyết định device có được tin cậy hay không.

Entity `Device`:

- `user`
- `fingerprint`
- `label`
- `status`: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`
- `approvedBy`
- `approvedAt`
- `expiresAt`

Flow hiện tại:

- Device lạ -> backend tạo row `PENDING`, login fail.
- Device không `APPROVED` -> login fail.
- Device seed được `APPROVED` trong migration demo.

Chưa thấy API approve/reject device trong code hiện tại. Cần hỏi leader/backend owner trước khi tự thêm.

## Role/permission

Entity:

- `Role`
- `Permission`
- `UserRole`
- `RolePermission`

Repository:

- `RoleRepository.findRoleNamesByUserId`
- `PermissionRepository.findPermissionNamesByUserId`

Authority runtime:

- Role thêm prefix `ROLE_`.
- Permission thêm prefix `PERM_`.

Seed role hiện có:

- `SYSTEM_ADMIN`
- `PARKING_MANAGER`
- `STAFF`
- `PARKING_USER`

## Rate limit

Annotation `@RateLimit` nằm ở `common/security/annotation/RateLimit.java`.

Aspect `RateLimitAspect` hỗ trợ:

- `USER_ID`
- `REQUEST_FIELD`
- `IP_ADDRESS` chưa implement logic riêng, default đang cho qua.

Login dùng:

```java
@RateLimit(limit = 5, duration = 60, type = RateLimit.Type.REQUEST_FIELD, fieldName = "#request.username")
```

Rate limit dùng Redis token bucket với Lua script `src/main/resources/scripts/token-bucket.lua`. Nếu Redis lỗi, `RateLimitService` fail-open và cho request đi qua.

## Redis trong auth

Redis key pattern ở `RedisKeys`:

- `smartpark:sess:authz:<sessionId>`
- `smartpark:sess:revoked:<sessionId>`
- `smartpark:sess:active:<sessionId>`
- `smartpark:ratelimit:user:<userId>`
- `smartpark:ratelimit:login:<username>`

Authz cache TTL được tính theo expiration của access token. Active marker TTL hiện là 60 giây. Revoked marker TTL thường theo access token TTL còn lại để access token đã logout không dùng tiếp được.

## Common errors

401 do access token hết hạn:

- Gọi `/auth/refresh`.
- Nếu refresh token cũng hết hạn hoặc session expired thì login lại.

Refresh cookie không gửi:

- Kiểm tra CORS credentials.
- Kiểm tra `SameSite=None` và `Secure`.
- Local HTTP với `cookie.secure=true` thường không gửi cookie.
- Có thể dùng `X-Refresh-Token` khi test.

Device chưa approved:

- Response thường là `DEVICE_NOT_TRUST`.
- Kiểm tra bảng `devices`.
- Dùng đúng seed device fingerprint khi demo.

Role thiếu prefix:

- DB lưu role là `SYSTEM_ADMIN`, code dùng `hasRole('SYSTEM_ADMIN')`.
- Không viết `hasRole('ROLE_SYSTEM_ADMIN')`.

Permission cache stale:

- Role/permission đổi nhưng cache authz theo session chưa clear.
- Cần clear `smartpark:sess:authz:<sessionId>` hoặc logout/relogin. Khi làm API phân quyền cần thiết kế invalidation.

Logout rồi token vẫn dùng được:

- Kiểm tra `sessions.revoked_at`.
- Kiểm tra Redis revoked marker.
- Kiểm tra `SessionGuardService.ensureActive`.
- Nếu Redis down, DB fallback vẫn phải chặn session revoked.
