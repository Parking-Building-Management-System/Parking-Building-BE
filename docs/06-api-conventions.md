# 06. API Conventions

## Quy ước tạo API mới

Khi làm API mới, mở các file nền trước:

- `common/response/ApiResponse.java`
- `common/exception/ErrorCode.java`
- `common/exception/ApiException.java`
- `common/exception/GlobalExceptionHandler.java`
- `common/security/config/SecurityConfig.java`
- Entity/repository/service/controller cùng module nếu đã tồn tại.

Luồng đúng:

```txt
Controller -> Request DTO -> Service -> Repository -> Entity -> Mapper -> Response DTO -> ApiResponse
```

Không bỏ qua service. Không trả entity trực tiếp.

## Cấu trúc package/module đề xuất

Theo convention hiện tại của `modules/identity`, module mới nên có:

```txt
modules/<module>/
  controller/
  service/
    impl/
  repository/
  entity/
  dto/
    request/
    response/
  mapper/
  enumType/
```

Ví dụ module vehicle:

```txt
modules/vehicle/controller/VehicleCategoryController.java
modules/vehicle/service/VehicleCategoryService.java
modules/vehicle/service/impl/VehicleCategoryServiceImpl.java
modules/vehicle/repository/VehicleCategoryRepository.java
modules/vehicle/entity/VehicleCategory.java
modules/vehicle/dto/request/CreateVehicleCategoryRequest.java
modules/vehicle/dto/request/UpdateVehicleCategoryRequest.java
modules/vehicle/dto/response/VehicleCategoryResponse.java
modules/vehicle/mapper/VehicleCategoryMapper.java
```

Cần hỏi leader/backend owner nếu muốn đặt vehicle dưới `modules/parking` thay vì module riêng.

## Endpoint naming

Hiện code có:

- `/auth/login`, `/auth/refresh`, `/auth/me`
- `/tenants`
- `/tenants/{id}`
- `/tenants/{id}/suspend`

Chưa thấy versioning `/api/v1` ở controller hiện tại, dù `SecurityConfig` có permit thêm `/api/v1/auth/login` và `/api/v1/auth/refresh`. Nếu thêm API mới, cần confirm với FE/leader có dùng prefix `/api/v1` hay không.

Quy tắc RESTful đề xuất:

- Dùng plural nouns: `/tenants`, `/permissions`, `/vehicle-categories`.
- GET list: `GET /resources`.
- GET detail: `GET /resources/{id}`.
- Create: `POST /resources`.
- Update toàn phần: `PUT /resources/{id}`.
- Update một phần/trạng thái: `PATCH /resources/{id}` hoặc `PATCH /resources/{id}/status`.
- Delete: `DELETE /resources/{id}`.

Nếu contract chưa rõ, ghi "cần confirm API contract với FE/leader".

## Pagination/filter/sort

Code hiện có `PageResponse<T>` nhưng chưa thấy API paging. Khi làm list API:

- Request dùng query params: `page`, `size`, `sort`, filter field.
- Page index nên thống nhất 0-based vì Spring `Pageable` mặc định 0-based.
- Response nên dùng `ApiResponse<PageResponse<XResponse>>`.
- Validate `page >= 0`, `1 <= size <= 100`.
- Nếu sort field không nằm whitelist, throw `ApiException(ErrorCode.INVALID_SORT_FIELD)`.

## Response

Luôn dùng `ApiResponse`.

Success:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

Error:

```json
{
  "code": 4000,
  "message": "Validation failed",
  "errors": {
    "name": "Tên không được để trống"
  },
  "timestamp": "2026-05-18T..."
}
```

Không trả entity trực tiếp vì:

- Dễ lộ field nội bộ như password, isDeleted.
- Dễ bị lazy loading/JSON loop.
- FE phụ thuộc DB schema.

## Validation

Controller nhận DTO phải có `@Valid`:

```java
public ResponseEntity<ApiResponse<XResponse>> create(@Valid @RequestBody CreateXRequest request)
```

Request DTO đặt annotation:

- `@NotBlank` cho string bắt buộc.
- `@NotNull` cho object/enum/id bắt buộc.
- `@Email` cho email.
- `@Pattern` cho slug/code.
- `@Size` nếu có giới hạn length.

Không validate thủ công trong controller nếu annotation xử lý được.

## Exception

Service throw:

```java
throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Khách hàng đã tồn tại");
```

Không return lỗi thủ công lung tung trong controller. Để `GlobalExceptionHandler` format.

Nếu thiếu mã lỗi phù hợp, thêm vào `ErrorCode` sau khi cân nhắc ảnh hưởng FE.

## Transaction

Write operation dùng `@Transactional` ở service implementation:

- create
- update
- delete/soft delete
- revoke session
- rotate refresh token

Read operation có thể dùng `@Transactional(readOnly = true)` nếu:

- Query nhiều repository cần consistency.
- Mapper cần đọc lazy relation.
- Muốn document rõ method không ghi DB.

Không đặt transaction ở controller.

## Mapper

Quy tắc:

- Entity -> response DTO qua mapper.
- Request DTO -> entity qua mapper hoặc build trong service nếu cần default/business rule.
- Không expose entity.

MapStruct hiện đã cấu hình annotation processor trong `pom.xml`. Mapper mới nên dùng:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface XMapper {
  XResponse toResponse(X entity);
}
```

## Naming

Đặt tên file/class:

- `CreateXRequest`
- `UpdateXRequest`
- `XStatusUpdateRequest`
- `XResponse`
- `XController`
- `XService`
- `XServiceImpl`
- `XRepository`
- `XMapper`

Repo hiện có `TenantCreationRequest`; nếu module cũ đã dùng kiểu này thì theo module cũ, nhưng module mới nên thống nhất `CreateXRequest` nếu team đồng ý.

## Security

Mặc định endpoint không public sẽ cần Bearer JWT do `SecurityConfig`.

Nếu endpoint cần quyền cụ thể:

```java
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@PreAuthorize("hasAuthority('PERM_QUYEN_TREN_APP.HE_THONG.TENANT.VIEW')")
```

Lưu ý hiện `TenantController` chưa có `@PreAuthorize` dù Swagger tag ghi dành cho System Admin. Cần confirm với leader/backend owner trước khi mở rộng tenant APIs.

## Cache

Read API có thể cache nếu:

- Được gọi nhiều.
- Data ít thay đổi.
- Có key rõ ràng.
- Có invalidation rõ ràng khi write.

Cache key phải thêm helper trong `RedisKeys`.

Write API phải invalidate:

- detail key.
- list/search key.
- dashboard counter key.
- authz/session key nếu thay đổi role/permission/user/tenant status.

## Common errors

- Controller chứa business logic.
- Service trả Entity ra ngoài.
- Repository chứa logic nghiệp vụ.
- Thiếu `@Valid`.
- DTO thiếu annotation nên dữ liệu rác vào DB.
- Thiếu `@PreAuthorize` cho endpoint quản trị.
- Dùng sai `hasRole('ROLE_X')` thay vì `hasRole('X')`.
- Thiếu cache invalidation sau write.
- Sửa entity nhưng không tạo migration.
- Bịa field DB vì chưa đọc migration/entity.
