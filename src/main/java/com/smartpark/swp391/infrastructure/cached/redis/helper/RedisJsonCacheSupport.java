package com.smartpark.swp391.infrastructure.cached.redis.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class RedisJsonCacheSupport {

  StringRedisTemplate redis;
  ObjectMapper objectMapper;

  // helper to read a value if we input a key
  protected Optional<String> readRawValue(String key) {
    try {
      String json = redis.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(json);
    } catch (DataAccessException e) {
      log.warn("Redis read failed for key={}", key, e);
      try {
        redis.delete(key);
      } catch (Exception ignored) {
      }
      return Optional.empty();
    }
  }

  // helper to write a key,value to redis if we input key, value (json), ttl
  protected boolean writeValue(String key, String json, Duration ttl) {
    try {
      redis.opsForValue().set(key, json, ttl);
      return true;
    } catch (DataAccessException e) {
      log.warn("Redis write failed for key={}", key, e);
      return false;
    }
  }

  // helper to delete a key from redis
  protected boolean deleteKey(String key) {
    try {
      redis.delete(key);
      return true;
    } catch (DataAccessException e) {
      log.warn("Redis delete failed for key={}", key, e);
      return false;
    }
  }

  // helper to check if a key is exist in redis
  protected boolean hasKey(String key) {
    try {
      return Boolean.TRUE.equals(redis.hasKey(key));
    } catch (DataAccessException e) {
      log.warn("Redis hasKey failed for key={}", key, e);
      return false;
    }
  }

  // helper to check whether some ttl is valid
  protected boolean isValidTtl(Duration ttl) {
    return ttl != null && !ttl.isZero() && !ttl.isNegative();
  }

  // helper to parse a json string to a detail class (DTO)
  // use when: cache value is an object
  protected <T> Optional<T> deserialize(String json, Class<T> clazz) {
    try {
      return Optional.of(objectMapper.readValue(json, clazz));
    } catch (JsonProcessingException e) {
      log.warn("Fail to deserialize json to {}", clazz.getSimpleName(), e);
      return Optional.empty();
    }
  }

  // helper to parse a json string to a detail generic type (list, map, set)
  // use when: cache value is a List<>, Map<>
  protected <T> Optional<T> deserialize(String json, TypeReference<T> type) {
    try {
      return Optional.of(objectMapper.readValue(json, type));
    } catch (JsonProcessingException e) {
      log.warn("Fail to deserialize json (TypeReference)", e);
      return Optional.empty();
    }
  }

  // helper to parse a object to String for redis saving
  protected Optional<String> serialize(Object value) {
    try {
      return Optional.of(objectMapper.writeValueAsString(value));
    } catch (JsonProcessingException e) {
      log.error("Fail to serialize: {}", value, e);
      return Optional.empty();
    }
  }

  /**
   * Xóa tất cả các khóa (keys) trong Redis khớp với mẫu (pattern) được chỉ định một cách an toàn. *
   *
   * <p>Thay vì sử dụng lệnh {@code KEYS} (vốn có độ phức tạp O(N) và có thể gây block toàn bộ
   * server Redis single-threaded), phương thức này sử dụng lệnh {@code SCAN} để duyệt qua các khóa
   * mà không gây nghẽn. *
   *
   * <p>Để tối ưu hóa chi phí mạng (network round-trips), các khóa được gom lô (batch) và xóa cùng
   * lúc mỗi khi đạt giới hạn {@code batchSize}.
   *
   * @param pattern mẫu chuỗi để đối chiếu khóa cần xóa (ví dụ: "omnigrade:session:*")
   * @return tổng số lượng khóa đã được xóa thành công
   */
  protected long scanAndDelete(String pattern) {
    long totalDeleted = 0;
    final int batchSize = 1000;

    // Cấp phát trước dung lượng cho Set để tránh việc resize mảng liên tục
    // Effective Java - Tối ưu cấp phát bộ nhớ (Avoid Rehashing Overhead)
    // Mặc định java.util.HashSet khởi tạo capacity = 16, load factor = 0.75. Khi vượt quá 75%,
    // mảng nội bộ sẽ bị "resize" (nhân đôi) và "rehash" lại toàn bộ dữ liệu, gây lãng phí CPU
    // rất lớn.
    // Vì chúng ta biết trước vòng lặp sẽ gom chính xác tối đa batchSize (1000 phần tử), việc
    // định cỡ
    // trước giúp hệ thống tránh được hàng chục chu kỳ resize đắt đỏ.
    //
    // JAVA 21: Thay vì dùng new HashSet<>(1000) (sẽ bị resize ở phần tử thứ 751 do 1000 *
    // 0.75 = 750),
    // ta dùng phương thức factory mới của Java 19+ để nó tự tính toán sức chứa hoàn hảo
    // (tức là sức chứa = 1000 / 0.75 ~ 1334) để chứa trọn vẹn 1000 phần tử mà không bị resize
    // lần nào.
    Set<String> batch = HashSet.newHashSet(batchSize);
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(batchSize).build();

    // Đảm bảo Cursor luôn được đóng để tránh memory leak trên client.
    try (Cursor<String> cursor = redis.scan(options)) {
      while (cursor.hasNext()) {
        batch.add(cursor.next());

        // Khi lô đầy, thực hiện xóa một lần (Pipeline/Multi-key operation)
        if (batch.size() >= batchSize) {
          Long deleted = redis.delete(batch);
          if (deleted != null) {
            totalDeleted += deleted;
          }
          batch.clear(); // Reset lô để hứng đợt tiếp theo
        }
      }

      // Xử lý nốt những khóa còn sót lại trong lô cuối cùng
      if (!batch.isEmpty()) {
        Long deleted = redis.delete(batch);
        if (deleted != null) {
          totalDeleted += deleted;
        }
      }
    } catch (Exception e) {
      // Không ném Exception làm sập luồng gọi, chỉ log lại.
      // Đảm bảo tính chống chịu (resilience) của hệ thống.
      log.warn("Redis scan failed for pattern={}", pattern, e);
    }

    return totalDeleted;
  }
}
