package com.aidata.insights.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作门面：统一基于 {@link StringRedisTemplate}，值以 JSON 字符串存储
 *
 * <p>不使用 {@code RedisTemplate<String, Object>}，避免写入 {@code @class} 类型信息；
 * 读取时显式传入目标类型，缓存数据在 Redis 中始终是可读的 JSON。</p>
 *
 * <p><b>禁止在业务代码中直接注入 {@code RedisTemplate} / {@code StringRedisTemplate}。</b>
 * 容器中仍存在 Spring Boot 自动配置的 {@code RedisTemplate<Object, Object>}（JDK 序列化），
 * 泛型擦除后 {@code @Autowired RedisTemplate<String, Object>} 能注入成功，会在 Redis 中写入
 * {@code \xAC\xED} 开头的二进制数据，与本类写入的 JSON 互不兼容。
 * 该约束由 {@code RedisArchitectureTest} 在构建阶段自动校验，违反时 {@code mvn test} 失败。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 写入对象，永不过期
     */
    public void set(String key, Object value) {
        stringRedisTemplate.opsForValue().set(key, serialize(value));
    }

    /**
     * 写入对象并设置过期时间
     */
    public void set(String key, Object value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, serialize(value), ttl);
    }

    /**
     * 仅当 key 不存在时写入，常用于幂等 / 简易分布式锁
     */
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, serialize(value), ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 读取并反序列化为指定类型
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        return Optional.ofNullable(deserialize(key, raw, type));
    }

    /**
     * 读取原始字符串
     */
    public Optional<String> get(String key) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        return StringUtils.hasText(raw) ? Optional.of(raw) : Optional.empty();
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    public long delete(Collection<String> keys) {
        Long count = stringRedisTemplate.delete(keys);
        return count == null ? 0L : count;
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, ttl));
    }

    /**
     * 剩余有效期（秒），-1 表示永不过期，-2 表示 key 不存在
     */
    public long getExpire(String key) {
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -2L : expire;
    }

    /**
     * 自增计数，常用于限流或统计
     */
    public long increment(String key, long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0L : value;
    }

    private String serialize(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis 缓存值不能为 null");
        }
        if (value instanceof CharSequence charSequence) {
            return charSequence.toString();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 序列化失败: " + value.getClass().getName(), e);
        }
    }

    private <T> T deserialize(String key, String raw, Class<T> type) {
        if (type == String.class) {
            return type.cast(raw);
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (JsonProcessingException e) {
            // 缓存脏数据不应让主流程失败，记录后按未命中处理
            log.warn("Redis 反序列化失败, 按未命中处理: key={}, type={}", key, type.getName());
            return null;
        }
    }
}
