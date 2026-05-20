# 02. Architecture Flow

## 1. Controller layer

Controller nằm ở `src/main/java/com/smartpark/swp391/modules/identity/controller`.

Hiện có:

- `AuthenticationController`: base path `/auth`.
- `TenantController`: base path `/tenants`.

Controller dùng:

- `@RestController`
- `@RequestMapping`
- `@PostMapping`, `@GetMapping`, `@PatchMapping`, `@DeleteMapping`
- `@RequestBody`
- `@PathVariable`
- `@CookieValue`
- `@RequestHeader`
- `@AuthenticationPrincipal`
- `@Valid`

Controller nên làm các việc:

- Nhận HTTP request.
- Validate request DTO bằng `@Valid`.
- Gọi service.
- Set HTTP header/cookie nếu cần.
- Trả `ResponseEntity<ApiResponse<...>>`.

Controller không nên:

- Query repository trực tiếp.
- Tự viết business rule phức tạp.
- Tự bắt lỗi nghiệp vụ bằng `try/catch` rồi return response thủ công.
- Trả JPA entity ra FE.

## 2. DTO layer

DTO nằm trong `src/main/java/com/smartpark/swp391/modules/identity/dto`.

Request DTO hiện có:

- `dto/authentication/request/AuthenticationRequest.java`: `username`, `password`, `deviceFingerprint`, `deviceLabel`.
- `dto/tenant/request/TenantCreationRequest.java`: `name`, `slug`, `emailContact`.
- `dto/token/request/TokenRequest.java`: DTO nội bộ cho token, không phải request public API.

Response DTO hiện có:

- `dto/authentication/response/AuthenticationResponse.java`: `authenticated`, `accessToken`, `refreshToken`.
- `dto/authentication/response/UserProfileResponse.java`: `id`, `tenantId`, `username`, `fullName`, `phone`, `roles`, `permissions`.
- `dto/tenant/response/TenantResponse.java`: `id`, `name`, `slug`, `emailContact`, `status`, `createdAt`.
- `dto/token/response/TokenPair.java`: DTO nội bộ service, gồm token và TTL.

Code hiện dùng cả Java record và Lombok class:

- `AuthenticationRequest`, `AuthenticationResponse`, `UserProfileResponse`, `TenantCreationRequest`, `TenantResponse` là `record`.
- `TokenRequest`, `TokenPair` là class dùng Lombok builder/getter/data.

Validation annotation hiện có:

- `@NotBlank` ở `AuthenticationRequest`.
- `@NotBlank`, `@Email`, `@Pattern` ở `TenantCreationRequest`.

## 3. Service layer

Service interface nằm ở:

- `modules/identity/service/AuthenticationService.java`
- `modules/identity/service/auth/SessionService.java`
- `modules/identity/service/auth/AuthorityLoader.java`
- `modules/identity/service/token/TokenService.java`
- `modules/identity/service/TenantService.java`

Implementation nằm ở:

- `modules/identity/service/auth/impl/AuthenticationServiceImpl.java`
- `modules/identity/service/auth/impl/SessionServiceImpl.java`
- `modules/identity/service/auth/impl/AuthorityLoaderImpl.java`
- `modules/identity/service/token/impl/TokenServiceImpl.java`
- `modules/identity/service/impl/TenantServiceImpl.java`

Business logic nằm ở service. Ví dụ:

- Check username/password/device trong `AuthenticationServiceImpl.authenticate`.
- Rotate refresh JTI trong `AuthenticationServiceImpl.refresh` và `SessionRepository.rotateRefreshJti`.
- Revoke session trong `SessionServiceImpl`.
- Cache-aside tenant trong `TenantServiceImpl.getTenantById`.
- Soft delete tenant trong `TenantServiceImpl.deleteTenant`.

`@Transactional` dùng khi:

- Có write DB: create session, revoke session, create tenant, suspend tenant, delete tenant.
- Cần đảm bảo nhiều thao tác DB nằm chung transaction.
- Read logic cần lazy relation trong transaction hoặc cần consistency, có thể dùng `@Transactional(readOnly = true)`.

## 4. Repository layer

Repository nằm ở `src/main/java/com/smartpark/swp391/modules/identity/repository`.

Tất cả repository hiện extends `JpaRepository<Entity, UUID>`.

Query method:

- `TenantRepository.existsBySlug`
- `TenantRepository.findBySlug`
- `UserRepository.findByUsername`

`@Query` hiện dùng cho:

- `UserRepository.findTenantIdByUserId`
- `RoleRepository.findRoleNamesByUserId`
- `PermissionRepository.findPermissionNamesByUserId`
- `DeviceRepository.findByUserIdAndFingerprint`
- `SessionRepository` rotate/revoke/check active/list active session.

Chưa thấy Specification/Pageable trong code hiện tại. Nếu làm API list/filter/sort cho Sprint 1, cần thêm `Pageable` hoặc `JpaSpecificationExecutor` theo convention mới, và cần confirm response paging với FE/leader.

## 5. Entity layer

Entity nằm ở `src/main/java/com/smartpark/swp391/modules/identity/entity`.

Các entity hiện có:

- `Tenant`
- `User`
- `Role`
- `Permission`
- `UserRole`
- `RolePermission`
- `Device`
- `Session`

Tất cả kế thừa `BaseEntity` ở `infrastructure/persistence/BaseEntity.java`, có:

- `id` UUIDv7.
- `createdAt`.
- `updatedAt`.
- `equals/hashCode` xử lý Hibernate proxy.

Relationship chính:

- `User` many-to-one `Tenant`.
- `Device` many-to-one `User`.
- `Session` many-to-one `User` và `Device`.
- `UserRole` many-to-one `User` và `Role`.
- `RolePermission` many-to-one `Role` và `Permission`.

Enum:

- `TenantStatus`: `ACTIVE`, `SUSPENDED`.
- `UserStatus`: `ACTIVE`, `INACTIVE`, `SUSPENDED`.
- `DeviceStatus`: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.

Audit fields:

- `created_at`, `updated_at` từ `BaseEntity`.

Khi sửa entity:

- Kiểm tra bảng trong migration.
- Tạo migration mới nếu đổi schema.
- Không dựa vào Hibernate auto DDL vì `ddl-auto: validate`.

## 6. Mapper layer

Mapper nằm ở `src/main/java/com/smartpark/swp391/modules/identity/mapper`.

Hiện có `TenantMapper`:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {
  TenantResponse toResponse(Tenant tenant);
}
```

Quy tắc:

- Entity -> response DTO qua mapper.
- Request DTO -> entity có thể qua mapper hoặc build thủ công trong service nếu logic đơn giản.
- Không trả entity trực tiếp ra API.
- Nếu DTO field không trùng entity field, khai báo mapping rõ ràng, không để FE nhận null do mapper bỏ qua.

## 7. Exception layer

Exception nằm ở `src/main/java/com/smartpark/swp391/common/exception`.

- `ErrorCode`: enum code/message/http status.
- `ApiException`: custom runtime exception chứa `ErrorCode`.
- `GlobalExceptionHandler`: `@RestControllerAdvice`, gom lỗi về `ApiResponse`.
- `ErrorResponse`: record cũ/khác format, hiện flow chính đang dùng `ApiResponse`.

Format lỗi phổ biến:

```json
{
  "code": 4000,
  "message": "Validation failed",
  "errors": {
    "slug": "Slug chỉ được chứa chữ thường, số và dấu gạch ngang"
  },
  "timestamp": "2026-05-18T...",
  "path": "/tenants"
}
```

## 8. Response wrapper

`ApiResponse<T>` nằm ở `common/response/ApiResponse.java`.

Field:

- `code`
- `message`
- `result`
- `errors`
- `timestamp`
- `path`

`PageResponse<T>` nằm ở `common/response/PageResponse.java`, gồm:

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`

Hiện chưa thấy controller nào dùng `PageResponse`.

## Ví dụ flow cụ thể: POST /auth/login

```txt
Client POST /auth/login
Body: AuthenticationRequest(username, password, deviceFingerprint, deviceLabel)
-> AuthenticationController.login
   - @Valid request
   - @RateLimit theo #request.username
-> AuthenticationServiceImpl.authenticate
   - UserRepository.findByUsername
   - PasswordEncoder.matches
   - check UserStatus.ACTIVE và TenantStatus.ACTIVE
   - DeviceRepository.findByUserIdAndFingerprint
   - nếu device lạ: save Device PENDING, throw DEVICE_NOT_TRUST
   - nếu device không APPROVED: throw DEVICE_NOT_TRUST
   - SessionService.createSession
-> SessionServiceImpl.createSession
   - tạo Session expiredAt, refreshJti
   - SessionRepository.save
-> TokenServiceImpl.generateTokenPair
   - access token: user_id, tenant_id, session_id, jti, exp
   - refresh token: thêm typ=REFRESH, jti=session.refreshJti
-> AuthenticationController
   - set refresh_token HttpOnly cookie
   - build AuthenticationResponse
   - return ApiResponse<AuthenticationResponse>
```

Điểm cần nhớ: access token không chứa role/permission. Role/permission được resolve mỗi request qua `JwtAuthenticationConverter` -> `SessionAuthorityResolver` -> Redis/DB.
