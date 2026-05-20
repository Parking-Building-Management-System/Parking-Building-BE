# 05. Cache And Redis Flow

Project hiện có Redis/cache rõ ràng.

## Redis config

Config nằm ở:

- `src/main/resources/application.yaml`
- `src/main/java/com/smartpark/swp391/infrastructure/cached/config/RedisConfig.java`

`application.yaml` đọc:

- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATA_REDIS_TIMEOUT`

`RedisConfig` tạo:

- `StringRedisTemplate`
- `ObjectMapper` cho Redis, có `JavaTimeModule`, tắt timestamp date numeric, ignore unknown properties.

## Cache helper

File: `src/main/java/com/smartpark/swp391/infrastructure/cached/redis/helper/RedisJsonCacheSupport.java`

Helper này cung cấp:

- `readRawValue`
- `writeValue`
- `deleteKey`
- `hasKey`
- `deserialize`
- `serialize`
- `scanAndDelete`

Khi viết cache service mới, nên kế thừa helper này nếu cache value là JSON DTO.

## Key pattern đang dùng

File: `src/main/java/com/smartpark/swp391/infrastructure/cached/redis/keys/RedisKeys.java`

Key hiện có:

- `smartpark:sess:authz:<sessionId>`: cache roles/permissions/tenant theo session.
- `smartpark:sess:revoked:<sessionId>`: marker session đã revoked.
- `smartpark:sess:active:<sessionId>`: marker session active.
- `smartpark:tenant:detail:<tenantId>`: cache tenant detail.
- `smartpark:ratelimit:user:<userId>`: rate limit theo user.
- `smartpark:ratelimit:login:<username>`: rate limit login theo username.

Ghi chú cần kiểm tra lại trong code hiện tại: `tenantPattern(tenantId)` trả `smartpark:tenant:<tenantId>:*`, trong khi `tenantDetail(id)` là `smartpark:tenant:detail:<id>`. `TenantCacheService.evictTenantData` có xóa trực tiếp `tenantDetail`, nên tenant detail vẫn bị xóa, nhưng pattern này sẽ không match key detail hiện tại. Nếu sau này thêm tenant-scoped cache, cần thống nhất pattern.

## Module dùng cache

Auth/session:

- `SessionAuthorityCacheService`
- `SessionAuthorityResolver`
- `SessionGuardService`
- `SessionServiceImpl`

Permission/role cache:

- Không cache permission độc lập theo user/role.
- Cache authorization theo session trong `SessionAuthzCache`, gồm `userId`, `tenantId`, `roles`, `permissions`.

Tenant:

- `TenantCacheService`
- `TenantServiceImpl.getTenantById` dùng cache-aside.
- `suspendTenant` và `deleteTenant` invalidate tenant cache.

Rate limit:

- `RateLimitService`
- `RateLimitAspect`
- Lua script `src/main/resources/scripts/token-bucket.lua`.

Dashboard counters:

- Chưa thấy dashboard module trong code hiện tại.
- Nếu Sprint 1 thêm dashboard counters, cân nhắc cache key riêng và TTL ngắn.

Revoked token/session:

- Revocation theo session, không theo JWT jti access token.
- Redis marker `smartpark:sess:revoked:<sessionId>` giúp chặn access token cũ sau logout.

## TTL ở đâu

Session authz:

- TTL tính theo access token expiration trong `SessionAuthorityResolver`.

Session active marker:

- `SessionGuardService.ACTIVE_TTL = 60 seconds`.

Session revoked marker:

- `SessionServiceImpl.revoke` dùng access token TTL truyền vào.
- `revokeAll` hiện dùng `Duration.ofMinutes(15)`. Cần kiểm tra lại nếu `JWT_VALID_DURATION` không phải 15 phút.

Tenant detail:

- `TenantCacheService.saveTenant` dùng `Duration.ofHours(1)`.

Rate limit:

- Annotation khai báo `limit` và `duration`.
- Lua script tính TTL bucket bằng `ceil(capacity / rate) * 2`.

## Flow cache-aside

Ví dụ `TenantServiceImpl.getTenantById`:

```txt
1. Tạo key smartpark:tenant:detail:<tenantId>.
2. Check Redis.
3. Nếu hit: trả TenantResponse.
4. Nếu miss:
   - TenantRepository.findById
   - map Tenant -> TenantResponse
   - TenantCacheService.saveTenant TTL 1h
   - return response
```

Áp dụng cho read API mới:

```txt
1. Validate input.
2. Tạo key nhất quán ở RedisKeys.
3. Check cache.
4. Miss thì query DB.
5. Map sang response DTO.
6. Set cache với TTL rõ ràng.
7. Return DTO.
```

## Flow invalidate

Write API phải clear cache liên quan:

```txt
create/update/delete DB
-> clear detail key
-> clear list/search/dashboard key liên quan nếu có
-> clear authz/session cache nếu quyền hoặc trạng thái user/tenant thay đổi
```

Hiện có:

- `TenantServiceImpl.suspendTenant` gọi `tenantCacheService.evictTenantData(id)`.
- `TenantServiceImpl.deleteTenant` gọi `tenantCacheService.evictTenantData(id)`.
- `SessionServiceImpl.revoke` clear authz/active và mark revoked.
- `SessionServiceImpl.revokeAll` clear từng session.

Khi update role/permission:

- Cần invalidate `smartpark:sess:authz:<sessionId>` cho user bị ảnh hưởng hoặc bắt user logout/relogin.
- Code hiện chưa có API role/permission CRUD nên cần thiết kế invalidation.

## Common errors

Data cũ do cache stale:

- Dấu hiệu: DB đã update nhưng API vẫn trả dữ liệu cũ.
- Kiểm tra write service có gọi evict không.
- Kiểm tra key detail/list/dashboard có được clear đủ không.

Key không nhất quán:

- Tất cả key mới nên thêm helper trong `RedisKeys`.
- Không tự nối string rải rác trong service.

Quên TTL:

- Cache không TTL có thể giữ data cũ quá lâu.
- Mọi `writeValue` nên có `Duration` rõ ràng.

Cache null value không kiểm soát:

- Hiện helper không cache null.
- Nếu muốn cache not-found, cần convention riêng và TTL ngắn.

Local không chạy Redis:

- Rate limit fail-open.
- Authz cache có fallback DB.
- Nhưng log sẽ nhiễu và performance khác production. Local nên chạy Redis bằng `docker compose up -d redis`.

Permission cache stale:

- Nếu thêm API update role/permission nhưng không clear authz cache, user có thể giữ quyền cũ tới khi access token hết hạn hoặc session cache bị clear.
