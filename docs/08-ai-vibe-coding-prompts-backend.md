# 08. AI Vibe Coding Prompts Backend

Dùng các prompt này khi làm với Codex/AI. Luôn yêu cầu AI đọc code hiện tại trước, không bịa entity/field/API.

## 1. Đọc hiểu module trước khi code

```txt
Bạn đang làm trong backend SmartPark dùng Spring Boot 3, Java 21, Spring Security, JPA, PostgreSQL, Flyway, Redis, Maven.

Trước khi code hãy đọc:
- Controller/Service/Repository/Entity/DTO/Mapper của module liên quan
- ApiResponse/ErrorCode/ApiException/GlobalExceptionHandler
- SecurityConfig/JwtAuthenticationConverter nếu endpoint cần quyền
- Migration Flyway liên quan

Nhiệm vụ:
- Giải thích flow hiện tại của module [tên module] theo Controller -> DTO -> Service -> Repository -> Entity -> Mapper -> ApiResponse.

Ràng buộc:
- Không sửa code.
- Không bịa entity/field/API.
- Nếu thiếu thông tin, dừng và liệt kê câu hỏi cần confirm.

Sau khi phân tích:
- Tóm tắt file đã đọc.
- Tóm tắt điểm cần chú ý trước khi code.
```

## 2. Tạo API GET list có pagination/filter/sort

```txt
Bạn đang làm trong backend SmartPark Spring Boot.

Trước khi code hãy đọc module [X], PageResponse, ApiResponse, ErrorCode và SecurityConfig.

Nhiệm vụ:
- Tạo API GET list [X] có page/size/sort/filter.

Ràng buộc:
- Không trả Entity trực tiếp.
- Dùng ApiResponse<PageResponse<XResponse>>.
- Validate page >= 0, size 1-100.
- Whitelist sort field, sort field sai thì dùng ErrorCode phù hợp.
- Không bịa field filter; nếu contract chưa rõ thì dừng hỏi.
- Business logic nằm ở Service, Repository chỉ query.

Sau khi code:
- Chạy build/test phù hợp.
- Tóm tắt file đã sửa và endpoint request/response.
```

## 3. Tạo API GET detail

```txt
Bạn đang làm trong backend SmartPark.

Đọc trước Controller/Service/Repository/Entity/Mapper/Response DTO của module [X].

Nhiệm vụ:
- Tạo API GET detail [X] theo id.

Ràng buộc:
- Not found thì throw ApiException(ErrorCode.RESOURCE_NOT_FOUND).
- Không trả Entity.
- Dùng Mapper sang XResponse.
- Nếu endpoint cần quyền, thêm @PreAuthorize đúng authority hiện tại.
- Nếu có cache Redis, dùng cache-aside theo RedisKeys.

Sau khi code hãy tóm tắt file sửa, response mẫu và test/build đã chạy.
```

## 4. Tạo API POST create

```txt
Bạn đang làm trong backend SmartPark.

Đọc trước entity/migration/repository/service/controller hiện có của module [X].

Nhiệm vụ:
- Tạo API POST create [X].

Ràng buộc:
- Tạo CreateXRequest với Jakarta Validation.
- Không bịa field DB; field phải khớp entity/migration.
- Service dùng @Transactional.
- Check duplicate nếu có unique constraint.
- Không viết business logic trong Controller.
- Dùng ApiResponse và AppException/ErrorCode.
- Nếu update DB schema, tạo migration mới, không sửa migration cũ.

Sau khi code chạy build/test phù hợp và tóm tắt file sửa.
```

## 5. Tạo API PUT/PATCH update

```txt
Bạn đang làm trong backend SmartPark.

Đọc trước entity/migration và update pattern hiện có.

Nhiệm vụ:
- Tạo API update [X].

Ràng buộc:
- Tạo UpdateXRequest.
- Find entity trong Service, not found dùng RESOURCE_NOT_FOUND.
- Validate unique field nếu update field unique.
- Service @Transactional.
- Không trả Entity.
- Nếu có cache detail/list/dashboard, invalidate sau update.
- Nếu thiếu rule status/field nullable, dừng hỏi.

Sau khi code tóm tắt endpoint và test/build.
```

## 6. Tạo API update status/toggle active

```txt
Bạn đang làm trong backend SmartPark.

Đọc enum status, entity, migration, service hiện có và cache/session flow nếu status ảnh hưởng quyền truy cập.

Nhiệm vụ:
- Tạo API update status cho [X].

Ràng buộc:
- Không tự nghĩ status enum mới.
- Nếu status ảnh hưởng user/session/tenant, kiểm tra cần revoke session hoặc clear cache.
- Validate transition nếu business có rule; nếu chưa rõ thì hỏi.
- Dùng @Transactional và ApiResponse.

Sau khi code tóm tắt impact tới cache/security.
```

## 7. Tạo API DELETE soft delete/hard delete

```txt
Bạn đang làm trong backend SmartPark.

Đọc entity [X], migration, relation/FK và convention xóa hiện có.

Nhiệm vụ:
- Tạo API delete [X].

Ràng buộc:
- Không quyết định soft delete hay hard delete nếu chưa có convention; dừng hỏi leader.
- Nếu soft delete, entity/table phải có field hỗ trợ hoặc tạo migration mới.
- Nếu hard delete, kiểm tra FK/relation đang dùng.
- Invalidate cache sau delete.
- Dùng ApiResponse<Void>.

Sau khi code tóm tắt rủi ro relation và test/build.
```

## 8. Thêm field/entity/migration

```txt
Bạn đang làm trong backend SmartPark dùng Flyway và Hibernate ddl-auto validate.

Đọc trước entity, migration cũ, repository/service/DTO liên quan.

Nhiệm vụ:
- Thêm [field/entity] cho [module].

Ràng buộc:
- Không sửa migration cũ đã chạy.
- Tạo migration Flyway mới theo convention VyyyyMMddHHmmss__description.sql.
- Entity annotation phải khớp DB nullable/length/enum.
- Update DTO/mapper/service nếu field được expose.
- Không bịa field nếu API contract chưa rõ.

Sau khi code chạy build/test hoặc giải thích nếu chưa chạy.
```

## 9. Thêm mapper DTO/entity

```txt
Bạn đang làm trong backend SmartPark dùng MapStruct.

Đọc DTO, entity và mapper hiện có.

Nhiệm vụ:
- Thêm mapper cho [X].

Ràng buộc:
- Dùng @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE).
- Mapping field khác tên phải khai báo rõ.
- Không map field sensitive như password ra response.
- Không dùng Entity làm response.

Sau khi code chạy build để đảm bảo MapStruct generate được.
```

## 10. Thêm validation + error code

```txt
Bạn đang làm trong backend SmartPark.

Đọc request DTO, ErrorCode, ApiException, GlobalExceptionHandler.

Nhiệm vụ:
- Thêm validation cho [API/DTO] và ErrorCode nếu cần.

Ràng buộc:
- Ưu tiên Jakarta Validation annotation trong request DTO.
- Không validate thủ công trong Controller nếu annotation đủ.
- ErrorCode mới phải có code/message/http status rõ.
- Không đổi code/message cũ nếu FE đã dùng mà chưa confirm.

Sau khi code tóm tắt behavior lỗi 400/409/403/404.
```

## 11. Thêm permission/security annotation

```txt
Bạn đang làm trong backend SmartPark.

Đọc SecurityConfig, JwtAuthenticationConverter, seed permissions, controller endpoint liên quan.

Nhiệm vụ:
- Thêm permission/security cho endpoint [X].

Ràng buộc:
- Role dùng hasRole('SYSTEM_ADMIN'), không thêm ROLE_ trong expression.
- Permission dùng hasAuthority('PERM_<permission_name>').
- Không invent permission name nếu seed/schema chưa có; hỏi leader.
- Swagger/docs phải cập nhật auth requirement.

Sau khi code test 401/403 nếu phù hợp.
```

## 12. Thêm cache Redis cho read API

```txt
Bạn đang làm trong backend SmartPark có RedisJsonCacheSupport và RedisKeys.

Đọc RedisConfig, RedisKeys, cache service hiện có và service read API [X].

Nhiệm vụ:
- Thêm cache Redis cache-aside cho read API [X].

Ràng buộc:
- Key phải khai báo trong RedisKeys.
- TTL rõ ràng.
- Cache DTO response, không cache Entity.
- Nếu Redis lỗi, API nên fallback DB nếu phù hợp.
- Không cache dữ liệu user-sensitive bằng key thiếu tenant/user scope.

Sau khi code tóm tắt key pattern, TTL và invalidation cần có.
```

## 13. Invalidate cache sau write API

```txt
Bạn đang làm trong backend SmartPark.

Đọc write service [X], RedisKeys, cache services liên quan.

Nhiệm vụ:
- Thêm invalidate cache sau create/update/delete [X].

Ràng buộc:
- Xóa detail/list/search/dashboard key liên quan.
- Nếu thay đổi role/permission/user/tenant status, xử lý session authz cache hoặc nêu câu hỏi confirm.
- Không dùng KEYS trực tiếp; dùng helper scanAndDelete nếu cần pattern.

Sau khi code tóm tắt key bị clear.
```

## 14. Viết Swagger/OpenAPI description/example

```txt
Bạn đang làm trong backend SmartPark dùng springdoc-openapi.

Đọc SwaggerConfig, controller và DTO của API [X].

Nhiệm vụ:
- Thêm OpenAPI docs cho API [X].

Ràng buộc:
- Không đổi business logic.
- Example request/response phải khớp DTO thật.
- Ghi rõ auth/permission.
- Ghi rõ error response phổ biến.
- Nếu enum/status, liệt kê giá trị.

Sau khi code mở Swagger hoặc chạy build nếu phù hợp.
```

## 15. Viết unit/integration test

```txt
Bạn đang làm trong backend SmartPark.

Đọc service/controller cần test và test hiện có.

Nhiệm vụ:
- Viết test cho [API/service].

Ràng buộc:
- Test success và lỗi chính.
- Với security, test 401/403 nếu endpoint protected.
- Với DB, ưu tiên Testcontainers nếu integration test cần PostgreSQL.
- Không phụ thuộc dữ liệu local ngoài migration/seed.

Sau khi code chạy ./mvnw test hoặc test class cụ thể.
```

## 16. Debug lỗi 401/403

```txt
Bạn đang debug backend SmartPark.

Đọc SecurityConfig, CustomJwtDecoder, JwtAuthenticationConverter, SessionAuthorityResolver, SessionGuardService, endpoint controller.

Nhiệm vụ:
- Debug lỗi 401/403 khi gọi [endpoint].

Ràng buộc:
- Không sửa code ngay.
- Xác định request có Bearer token không, token hết hạn không, session revoked không, device/user/tenant status thế nào.
- Với 403 kiểm tra role/permission authority thực tế.
- Không đoán; chỉ kết luận theo code/log/request.

Sau khi phân tích đề xuất fix nhỏ nhất.
```

## 17. Debug lỗi JPA relationship/lazy loading

```txt
Bạn đang debug backend SmartPark JPA.

Đọc entity relation, repository query, service transaction boundary, mapper/response.

Nhiệm vụ:
- Debug lỗi LazyInitializationException/JSON loop/null relation ở [API].

Ràng buộc:
- Không trả Entity trực tiếp.
- Ưu tiên DTO + mapper.
- Nếu cần fetch relation, dùng query rõ ràng hoặc transaction readOnly.
- Không đổi fetch LAZY sang EAGER toàn cục nếu chưa chứng minh cần.

Sau khi phân tích nêu root cause và patch nhỏ nhất.
```

## 18. Debug migration/database

```txt
Bạn đang debug migration/database backend SmartPark.

Đọc application.yaml, Flyway migration, entity liên quan và error log.

Nhiệm vụ:
- Debug lỗi migration/schema validate [mô tả lỗi].

Ràng buộc:
- Không sửa migration cũ đã chạy.
- Nếu schema cần đổi, tạo migration mới.
- Entity phải khớp nullable/length/enum/table/column.
- Không chạy command destructive với DB nếu chưa hỏi.

Sau khi phân tích tóm tắt nguyên nhân, file cần sửa và migration cần tạo.
```
