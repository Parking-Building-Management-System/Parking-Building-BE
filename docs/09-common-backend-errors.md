# 09. Common Backend Errors

## 401 Unauthorized

Dấu hiệu nhận biết:

- Response `code=4010`, message `Unauthenticated`.
- API protected trả 401.
- `/auth/me`, `/auth/logout`, `/tenants/**` không có Bearer token sẽ 401.

File nên kiểm tra:

- `common/security/config/SecurityConfig.java`
- `common/security/jwt/CustomJwtDecoder.java`
- `common/security/config/JwtAuthenticationConverter.java`
- `common/security/config/SessionGuardService.java`
- `modules/identity/service/token/impl/TokenServiceImpl.java`
- `modules/identity/entity/Session.java`

Cách debug:

- Kiểm tra header `Authorization: Bearer <accessToken>`.
- Kiểm tra access token hết hạn chưa.
- Kiểm tra claim `user_id`, `tenant_id`, `session_id`.
- Kiểm tra `sessions.revoked_at` và `sessions.expired_at`.
- Kiểm tra Redis revoked marker `smartpark:sess:revoked:<sessionId>`.

Cách sửa thường gặp:

- Login lại hoặc refresh token.
- Sửa FE gửi đúng Bearer token.
- Nếu logout rồi token vẫn dùng được, kiểm tra session guard/revoked marker.

## Refresh token/cookie issue

Dấu hiệu nhận biết:

- `/auth/refresh` trả 401.
- Browser không gửi cookie `refresh_token`.
- Header `X-Refresh-Token` hoạt động nhưng cookie không.

File nên kiểm tra:

- `AuthenticationController.java`
- `CorsConfig.java`
- `application.yaml`

Cách debug:

- Kiểm tra response login có `Set-Cookie`.
- Kiểm tra `cookie.secure`, `SameSite=None`.
- Kiểm tra FE có `withCredentials` hoặc `credentials: "include"`.
- Kiểm tra `CORS_ALLOWED_ORIGINS` đúng origin FE.

Cách sửa thường gặp:

- Local HTTP cần cấu hình cookie secure phù hợp hoặc dùng header khi test.
- FE bật credentials.
- Không dùng wildcard origin khi gửi credentials.

## Device chưa approved

Dấu hiệu nhận biết:

- Login trả `DEVICE_NOT_TRUST`, code `4005`.
- Bảng `devices` có row status `PENDING`.

File nên kiểm tra:

- `AuthenticationServiceImpl.authenticate`
- `Device.java`
- `DeviceRepository.java`
- Seed migration `V20260518093000__seed_identity_auth.sql`

Cách debug:

- Kiểm tra `deviceFingerprint` client gửi.
- Query `devices` theo `user_id` và `fingerprint`.
- Dùng seed fingerprint khi demo.

Cách sửa thường gặp:

- Approve device bằng flow/admin API khi có.
- Nếu chưa có API approve device, cần hỏi leader/backend owner.

## 403 Forbidden

Dấu hiệu nhận biết:

- Response `code=4030`, message `Forbidden`.
- Endpoint có `@PreAuthorize` bị chặn.

File nên kiểm tra:

- Controller endpoint.
- `JwtAuthenticationConverter.java`
- `RoleRepository.java`
- `PermissionRepository.java`
- Seed role/permission migration.

Cách debug:

- Với role, nhớ code map `ROLE_` tự động.
- `hasRole('SYSTEM_ADMIN')` đúng, `hasRole('ROLE_SYSTEM_ADMIN')` sai.
- Với permission, dùng `hasAuthority('PERM_<permission_name>')`.
- Kiểm tra authz cache có stale không.

Cách sửa thường gặp:

- Seed/gán đúng role/permission.
- Sửa expression `@PreAuthorize`.
- Clear session authz cache hoặc logout/relogin sau khi đổi quyền.

## 400 Validation

Dấu hiệu nhận biết:

- Response `code=4000`, message `Validation failed`.
- `errors` có map field -> message.

File nên kiểm tra:

- Request DTO.
- Controller có `@Valid` chưa.
- `GlobalExceptionHandler.handleMethodArgumentNotValid`.

Cách debug:

- Kiểm tra request field name có đúng DTO không.
- Kiểm tra DTO annotation `@NotBlank`, `@Email`, `@Pattern`.
- Kiểm tra JSON body có đúng Content-Type `application/json`.

Cách sửa thường gặp:

- Thêm `@Valid` ở controller.
- Thêm annotation validation vào DTO.
- Sửa FE field name theo DTO.

## 500 do NullPointerException

Dấu hiệu nhận biết:

- Response `code=5000`.
- Log có stacktrace NPE.

File nên kiểm tra:

- Service implementation.
- Mapper.
- Repository return `Optional`.
- Entity relation nullable.

Cách debug:

- Tìm dòng NPE trong log.
- Kiểm tra Optional có `orElseThrow` chưa.
- Kiểm tra mapper nhận null.
- Kiểm tra relation lazy/null.

Cách sửa thường gặp:

- Check null đúng chỗ trong service.
- Throw `ApiException(ErrorCode.RESOURCE_NOT_FOUND)` thay vì `.get()` Optional.
- Không map relation chưa load nếu không cần.

## LazyInitializationException

Dấu hiệu nhận biết:

- Log Hibernate lazy initialization.
- Thường xảy ra khi trả entity trực tiếp hoặc mapper đọc lazy relation ngoài transaction.

File nên kiểm tra:

- Entity relationship.
- Service transaction boundary.
- Mapper/response DTO.
- Controller return type.

Cách debug:

- Kiểm tra có trả entity ra API không.
- Kiểm tra mapper chạy ở service hay controller.
- Kiểm tra method service có `@Transactional(readOnly = true)` nếu cần.

Cách sửa thường gặp:

- Trả DTO.
- Query fetch rõ relation cần dùng.
- Map trong service.
- Không đổi tất cả relation sang EAGER.

## Migration fail

Dấu hiệu nhận biết:

- App fail start ở Flyway hoặc Hibernate validate.
- Lỗi table/column/constraint không tồn tại hoặc mismatch.

File nên kiểm tra:

- `src/main/resources/db/migration`
- Entity liên quan.
- `application.yaml` Flyway/JPA config.

Cách debug:

- Đọc migration theo thứ tự timestamp.
- Kiểm tra bảng/cột trong DB thật.
- So sánh nullable/length/enum giữa entity và SQL.

Cách sửa thường gặp:

- Tạo migration mới.
- Không sửa migration cũ đã chạy.
- Nếu local DB bẩn, reset local DB chỉ khi được phép, không làm trên DB shared.

## Duplicate key

Dấu hiệu nhận biết:

- Response có thể map về `DUPLICATE_RESOURCE`, code `4090`.
- Log `DataIntegrityViolationException`.

File nên kiểm tra:

- Migration unique constraints.
- Repository exists/find method.
- Service create/update.

Cách debug:

- Kiểm tra unique: `tenants.slug`, `users.username`, `roles.name`, `permissions.name`, `(user_id, fingerprint)`.
- Kiểm tra service có check trước khi save không.

Cách sửa thường gặp:

- Check duplicate trong service.
- Vẫn giữ DB unique constraint.
- Handle race condition bằng catch `DataIntegrityViolationException` nếu cần message riêng.

## Cache stale

Dấu hiệu nhận biết:

- DB đã đổi nhưng API vẫn trả data cũ.
- User vẫn có quyền cũ sau khi role/permission đổi.

File nên kiểm tra:

- `RedisKeys.java`
- Cache service liên quan.
- Write service có evict không.
- `SessionAuthorityCacheService.java`

Cách debug:

- Xác định key cache.
- Kiểm tra TTL.
- Kiểm tra write path có gọi evict.
- Với authz, kiểm tra `smartpark:sess:authz:<sessionId>`.

Cách sửa thường gặp:

- Invalidate detail/list key sau write.
- Clear authz cache hoặc force logout khi đổi role/permission.
- Thống nhất key pattern trong `RedisKeys`.

## CORS/cookie

Dấu hiệu nhận biết:

- Browser block request.
- Cookie không gửi dù login đã set.
- FE gọi refresh luôn 401.

File nên kiểm tra:

- `CorsConfig.java`
- `AuthenticationController.java`
- `application.yaml`

Cách debug:

- Kiểm tra origin FE có trong `CORS_ALLOWED_ORIGINS`.
- Kiểm tra FE bật credentials.
- Kiểm tra `SameSite=None` và `Secure`.

Cách sửa thường gặp:

- Set allowed origins chính xác.
- FE bật credentials.
- Local HTTP cân nhắc `cookie.secure=false` nếu môi trường cho phép.

## JSON serialization loop

Dấu hiệu nhận biết:

- Stack overflow khi serialize.
- Response lộ entity relation lồng nhau.

File nên kiểm tra:

- Controller return type.
- Entity bidirectional relation.
- DTO/mapper.

Cách debug:

- Tìm endpoint trả entity.
- Kiểm tra relation `User -> Tenant`, `Session -> User/Device`.

Cách sửa thường gặp:

- Luôn trả DTO.
- Mapper chọn field cần trả.
- Không expose entity graph.

## Pagination sai

Dấu hiệu nhận biết:

- FE nói page lệch 1.
- Sort field không tồn tại gây 500.
- Response thiếu metadata.

File nên kiểm tra:

- Controller query params.
- Service validate page/size/sort.
- `PageResponse.java`.

Cách debug:

- Xác nhận 0-based hay 1-based với FE.
- Whitelist sort field.
- Kiểm tra response có `totalElements`, `totalPages`.

Cách sửa thường gặp:

- Dùng Spring Pageable 0-based và document rõ.
- Validate sort field trước khi query.
- Trả `ApiResponse<PageResponse<T>>`.

## Timezone/Instant

Dấu hiệu nhận biết:

- Thời gian lệch giữa FE/BE/DB.
- Session hết hạn sớm/muộn.

File nên kiểm tra:

- `BaseEntity` dùng `LocalDateTime`.
- `TokenServiceImpl` dùng `Instant`/`Date`.
- Migration dùng `TIMESTAMP WITH TIME ZONE`.

Cách debug:

- Kiểm tra timezone JVM/container/DB.
- Kiểm tra response format.
- Kiểm tra session `expired_at`.

Cách sửa thường gặp:

- Thống nhất ISO string cho FE.
- Cẩn thận khi convert `LocalDateTime` sang `Instant`.
- Cần hỏi leader/backend owner nếu muốn chuẩn hóa toàn project sang `Instant`.

## Lombok/build IDE issue

Dấu hiệu nhận biết:

- IDE báo không thấy getter/builder.
- Maven build khác IDE.

File nên kiểm tra:

- `pom.xml` annotation processor.
- IDE annotation processing setting.

Cách debug:

- Chạy `./mvnw test` để xác nhận build thật.
- Kiểm tra Lombok plugin/annotation processing trong IDE.

Cách sửa thường gặp:

- Bật annotation processing.
- Import đúng Lombok annotation.

## MapStruct issue

Dấu hiệu nhận biết:

- Build fail không generate mapper.
- Mapper thiếu field mapping.

File nên kiểm tra:

- `pom.xml` annotation processor MapStruct.
- Mapper interface.
- DTO/entity field names.

Cách debug:

- Chạy Maven build.
- Kiểm tra generated sources.
- Kiểm tra field khác tên nhưng không có `@Mapping`.

Cách sửa thường gặp:

- Thêm `@Mapping`.
- Đảm bảo mapper có `componentModel = "spring"`.
- Không để response phụ thuộc field sensitive.
