# 01. How To Run Backend

## Công cụ cần có

Repo hiện dùng Maven wrapper, không thấy Gradle. Ưu tiên dùng `./mvnw` để tránh lệch version Maven cài global.

Cần chuẩn bị:

- JDK 21.
- Docker nếu muốn chạy PostgreSQL/Redis local bằng `compose.yaml`.
- PostgreSQL 16-compatible.
- Redis 7-compatible.
- Biến môi trường cho datasource, Redis và JWT.

## Chạy bằng Maven

```bash
./mvnw spring-boot:run
./mvnw clean package
./mvnw test
```

Lệnh tương đương nếu dùng Maven global:

```bash
mvn spring-boot:run
mvn clean package
mvn test
```

Repo không có `build.gradle`, nên không dùng:

```bash
./gradlew bootRun
./gradlew build
./gradlew test
```

## Profile và config

Config chính nằm ở `src/main/resources/application.yaml`.

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

Hiện `application.yaml` default profile là `dev`, nhưng trong `src/main/resources` chưa thấy `application-dev.yml`, `application-dev.yaml`, `application-prod.yml` hoặc `application-prod.yaml`. Nghĩa là nếu không có config ngoài hoặc biến môi trường, app vẫn dùng `application.yaml`.

Các biến quan trọng:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_TIMEOUT`
- `JWT_SIGNER_KEY_BASE64`
- `JWT_VALID_DURATION`
- `JWT_REFRESHABLE_DURATION`
- `SERVER_PORT`, default `8080`
- `CORS_ALLOWED_ORIGINS`, default `http://localhost:3000,http://localhost:5173`
- `SWAGGER_SERVER_URL`, default `http://localhost:8080`
- `SWAGGER_SERVER_DESC`, default `Local Server`

`AuthenticationController` còn đọc:

- `cookie.path`, default `/`
- `cookie.secure`, default `true`

Vì `cookie.secure` default `true`, local HTTP có thể không gửi cookie refresh token trong browser. Khi test local qua HTTP, cần cấu hình phù hợp hoặc dùng header `X-Refresh-Token` cho `/auth/refresh`.

## Database local

DB là PostgreSQL. `application.yaml` đang dùng:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Lưu ý quan trọng:

- `ddl-auto: validate`: Hibernate chỉ validate schema, không tự tạo/sửa bảng.
- Flyway chạy migration trong `src/main/resources/db/migration`.
- Nếu entity đổi mà không có migration mới, app có thể fail khi start.

## Redis local

Redis config nằm ở:

- `src/main/resources/application.yaml`
- `src/main/java/com/smartpark/swp391/infrastructure/cached/config/RedisConfig.java`

Redis được dùng cho:

- session authorization cache.
- session active marker.
- session revoked marker.
- tenant detail cache.
- rate limit token bucket.

Nếu Redis chưa chạy, một số flow có fallback DB, nhưng rate limit/cache sẽ log lỗi. Không nên coi Redis là optional ở môi trường team/dev chung.

## Docker compose

Repo có `compose.yaml` ở root, định nghĩa:

- `postgres`: image `postgres:16-alpine`, expose host port `5433` tới container port `5432`.
- `redis`: image `redis:7-alpine`, expose `6379`.

Chạy:

```bash
docker compose up -d
docker compose ps
docker compose logs postgres
docker compose logs redis
```

Biến `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` được compose lấy từ môi trường hoặc `.env`. Không ghi secret thật vào docs hoặc commit.

Ví dụ datasource local thường sẽ có dạng:

```txt
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/<POSTGRES_DB>
SPRING_DATASOURCE_USERNAME=<POSTGRES_USER>
SPRING_DATASOURCE_PASSWORD=<POSTGRES_PASSWORD>
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_TIMEOUT=2s
```

## Swagger/OpenAPI

Swagger config nằm ở `src/main/java/com/smartpark/swp391/common/config/SwaggerConfig.java`.

Mở local:

```txt
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

Swagger có Bearer JWT security scheme tên `bearerAuth`.

## Seed data

Seed auth nằm ở `src/main/resources/db/migration/V20260518093000__seed_identity_auth.sql`.

Các user seed dùng password `Password@123`:

- `system.admin@smartpark.local`, device `seed-system-admin-device`, role `SYSTEM_ADMIN`.
- `manager@demo-parking.local`, device `seed-manager-device`, role `PARKING_MANAGER`.
- `staff@demo-parking.local`, device `seed-staff-device`, role `STAFF`.
- `driver@demo-parking.local`, device `seed-driver-device`, role `PARKING_USER`.

## Common errors

Port backend bị chiếm:

- Dấu hiệu: app fail bind port `8080`.
- Kiểm tra `server.port` hoặc `SERVER_PORT`.
- Đổi `SERVER_PORT=8081` nếu cần.

DB chưa chạy:

- Dấu hiệu: connection refused tới PostgreSQL.
- Kiểm tra `docker compose ps`, `SPRING_DATASOURCE_URL`.
- Với compose hiện tại, host port là `5433`, không phải `5432`.

Redis chưa chạy:

- Dấu hiệu: log lỗi Redis/cache/rate limit.
- Kiểm tra `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`.

Migration fail:

- Dấu hiệu: Flyway error khi start.
- Kiểm tra file trong `src/main/resources/db/migration`.
- Không sửa migration cũ đã chạy ở DB shared; tạo migration mới.

Thiếu JWT secret/key:

- Dấu hiệu: fail decode Base64 hoặc lỗi init JWT decoder.
- Kiểm tra `JWT_SIGNER_KEY_BASE64`; HS512 cần key đủ mạnh.

CORS lỗi khi FE gọi:

- Kiểm tra `CORS_ALLOWED_ORIGINS`.
- `CorsConfig` bật `allowCredentials(true)`, nên origin không được dùng wildcard tùy tiện nếu gửi cookie.

Cookie SameSite/Secure sai local/prod:

- Code set refresh cookie `HttpOnly`, `SameSite=None`, `secure` theo `cookie.secure`.
- `SameSite=None` trên browser hiện đại thường cần `Secure`.
- Local HTTP với `Secure=true` có thể không gửi cookie.

Login 401/400 do device chưa approved:

- Login device lạ sẽ tạo `Device` status `PENDING` và trả `DEVICE_NOT_TRUST`.
- Dùng seed device fingerprint hoặc cần flow approve device. Cần hỏi leader/backend owner về API approve device nếu chưa có.
