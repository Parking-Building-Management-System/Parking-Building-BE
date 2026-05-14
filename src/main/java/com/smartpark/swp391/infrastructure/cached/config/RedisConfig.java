package com.smartpark.swp391.infrastructure.cached.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// No need complicated config since:
// Spring IoC auto get port, host, timeout in config file --> Auto create connectionFactory
// Later when deploy, we just need to change setting in config file, Spring auto dothe other
// --> We just create the template and inject that custom config to connect to our redis host
// Gate: StringRedisConfig<Key, Value> is the service that we read, write and work with redis

@Configuration
public class RedisConfig {
  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }

  @Bean
  public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Xử lý LocalDateTime của BaseEntity
    mapper.registerModule(new JavaTimeModule());
    // Thêm cột mới vào Entity không bị văng lỗi khi đọc cache cũ
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }
}
