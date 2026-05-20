# 00. Backend Overview

## Dự án backend này là gì

Repo này là backend Spring Boot cho SmartPark / Parking Building Management. Theo code hiện tại, backend đang tập trung vào nền tảng SaaS nhiều tenant, xác thực đăng nhập, session, phân quyền RBAC, quản lý tenant và cache Redis. Các nghiệp vụ bãi xe như dashboard, parking building, vehicle category, parking slot chưa thấy entity/controller/service rõ ràng trong code hiện tại, cần hỏi leader/backend owner trước khi implement.

Entry point của app nằm ở `src/main/java/com/smartpark/swp391/Swp391Application.java`.

## Stack đang dùng theo code thực tế

- Java 21: khai báo trong `pom.xml` bằng `java.version` và `maven.compiler.release`.
- Spring Boot 3.5.13: parent trong `pom.xml`.
- Maven: repo có `pom.xml`, `mvnw`, `mvnw.cmd`; không thấy Gradle.
- Spring Web: REST controller.
- Spring Security + OAuth2 Resource Server: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`.
- JWT tự ký HS512: `TokenServiceImpl`, `CustomJwtDecoder`, secret từ `jwt.signer-key-base64`.
- Spring Data JPA + Hibernate: entity/repository trong `modules/identity`.
- PostgreSQL: driver `org.postgresql:postgresql`, dialect trong `application.yaml`.
- Flyway: migration ở `src/main/resources/db/migration`.
- Redis: `spring-boot-starter-data-redis`, config ở `infrastructure/cached`.
- Swagger/OpenAPI: `springdoc-openapi-starter-webmvc-ui`, config ở `common/config/SwaggerConfig.java`.
- Lombok: dùng `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@FieldDefaults`.
- MapStruct: `TenantMapper` dùng `@Mapper(componentModel = "spring")`.
- Jakarta Validation: DTO request dùng `@Valid`, `@NotBlank`, `@Email`, `@Pattern`.
- Actuator, WebSocket, JobRunr, AWS S3 dependency có trong `pom.xml`, nhưng chưa thấy module nghiệp vụ tương ứng trong code hiện tại.

## Cấu trúc package tổng quan

- `com.smartpark.swp391.common.config`: cấu hình CORS và Swagger.
- `com.smartpark.swp391.common.security`: security core, JWT decoder, method security, rate limit.
- `com.smartpark.swp391.common.exception`: `ApiException`, `ErrorCode`, `GlobalExceptionHandler`.
- `com.smartpark.swp391.common.response`: `ApiResponse`, `PageResponse`.
- `com.smartpark.swp391.infrastructure.persistence`: `BaseEntity`, UUIDv7 generator.
- `com.smartpark.swp391.infrastructure.cached`: Redis config, Redis key convention, cache services.
- `com.smartpark.swp391.modules.identity`: controller/service/repository/entity/dto/mapper cho auth, session, RBAC, tenant.

## Khái niệm quan trọng cho người mới

Controller là lớp nhận HTTP request. Ví dụ `AuthenticationController` và `TenantController`. Controller chỉ parse input, gọi service, bọc kết quả bằng `ApiResponse`, không nên chứa business logic.

Service là lớp chứa nghiệp vụ. Ví dụ `AuthenticationServiceImpl`, `SessionServiceImpl`, `TenantServiceImpl`. Đây là nơi kiểm tra user/password/device, tạo session, revoke session, kiểm tra duplicate slug tenant, cache-aside.

Repository là lớp truy cập DB bằng Spring Data JPA. Ví dụ `UserRepository`, `SessionRepository`, `PermissionRepository`. Repository chỉ nên chứa query, không chứa luồng nghiệp vụ.

Entity là mapping bảng DB. Ví dụ `User`, `Tenant`, `Session`, `Device`. Entity kế thừa `BaseEntity`, có `id`, `createdAt`, `updatedAt`. Khi sửa entity phải kiểm tra migration Flyway tương ứng.

DTO là object request/response của API. Ví dụ `AuthenticationRequest`, `AuthenticationResponse`, `UserProfileResponse`, `TenantCreationRequest`, `TenantResponse`. Không trả entity trực tiếp ra API.

Mapper chuyển Entity sang DTO. Hiện có `TenantMapper` dùng MapStruct. Module mới nên tạo mapper riêng nếu response không đơn giản.

Exception handler là lớp gom lỗi về format thống nhất. `GlobalExceptionHandler` bắt `ApiException`, validation error, JSON malformed, access denied, DB constraint và unexpected error.

ApiResponse wrapper là format response chung ở `common/response/ApiResponse.java`, gồm `code`, `message`, `result`, `errors`, `timestamp`, `path`.

Security filter/config nằm ở `SecurityConfig`, `CustomJwtDecoder`, `JwtAuthenticationConverter`, `SessionAuthorityResolver`, `SessionGuardService`. Đây là core auth, không sửa tùy tiện.

Cache/Redis nằm ở `infrastructure/cached`. Redis đang dùng cho session authorization cache, revoked marker, active marker, tenant detail cache và rate limit.

Transaction dùng `@Transactional` ở service cho write operation hoặc read logic cần transaction boundary. Ví dụ `TenantServiceImpl.createTenant`, `AuthenticationServiceImpl.refresh`, `SessionServiceImpl.revokeAll`.

## Flow tổng quát

```txt
Client
-> Controller
-> Request DTO validation (@Valid)
-> Service
-> Repository
-> Database
-> Mapper
-> Response DTO
-> ApiResponse
```

Ví dụ tenant:

```txt
POST /tenants
-> TenantController.createTenant
-> @Valid TenantCreationRequest
-> TenantServiceImpl.createTenant
-> TenantRepository.existsBySlug + save
-> Tenant entity
-> TenantMapper.toResponse
-> TenantResponse
-> ApiResponse<TenantResponse>
```

Ví dụ auth:

```txt
POST /auth/login
-> AuthenticationController.login
-> @Valid AuthenticationRequest
-> AuthenticationServiceImpl.authenticate
-> UserRepository + DeviceRepository + SessionService
-> TokenServiceImpl
-> AuthenticationResponse
-> ApiResponse<AuthenticationResponse> + Set-Cookie refresh_token
```

## Package/file không nên sửa tùy tiện

- `common/security/config/SecurityConfig.java`: đổi matcher/filter chain có thể làm public/private endpoint sai.
- `common/security/jwt/CustomJwtDecoder.java`: verify JWT HS512.
- `common/security/config/JwtAuthenticationConverter.java`: map role/permission sang Spring authority.
- `common/security/config/SessionAuthorityResolver.java` và `SessionGuardService.java`: kiểm tra session active/revoked và cache authz.
- `common/exception/GlobalExceptionHandler.java`, `ErrorCode.java`, `ApiException.java`: đổi format lỗi sẽ ảnh hưởng FE.
- `common/response/ApiResponse.java`: đổi field response sẽ ảnh hưởng toàn bộ API.
- Migration trong `src/main/resources/db/migration` đã chạy: không sửa migration cũ, tạo migration mới.
- Entity relation `User`, `Tenant`, `Session`, `Device`, `UserRole`, `RolePermission`: sửa quan hệ JPA khi chưa hiểu nghiệp vụ dễ gây lỗi lazy loading, FK, migration.
