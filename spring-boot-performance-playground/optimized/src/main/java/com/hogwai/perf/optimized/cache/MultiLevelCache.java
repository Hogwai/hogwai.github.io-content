package com.hogwai.perf.optimized.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Multi-level cache: L1 (Caffeine, local JVM) -> L2 (Redis, cluster) -> DB (fallback).
 * Uses sync=true to prevent thundering herd on cache miss.
 * Requires Redis to be running (docker compose up redis).
 * When cache.multilevel.enabled=false, bypasses cache and delegates directly to DB.
 */
@Component
public class MultiLevelCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> l1Cache;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final boolean enabled;

    public MultiLevelCache(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            @Value("${cache.multilevel.enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .recordStats()
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, Supplier<T> dbFallback) {
        if (!enabled) {
            return dbFallback.get();
        }

        // L1: local Caffeine cache - ~0.05ms
        var l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            return (T) l1Value;
        }

        // L2: Redis - ~0.3ms
        if (redisTemplate != null) {
            var l2Value = redisTemplate.opsForValue().get(key);
            if (l2Value != null) {
                l1Cache.put(key, l2Value);
                return (T) l2Value;
            }
        }

        // Cache miss: DB fallback with thundering herd protection
        var lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-check after acquiring lock
            var recheck = l1Cache.getIfPresent(key);
            if (recheck != null) return (T) recheck;

            var dbValue = dbFallback.get();
            if (dbValue != null && redisTemplate != null) {
                redisTemplate.opsForValue().set(key, dbValue, Duration.ofMinutes(5));
                l1Cache.put(key, dbValue);
            } else if (dbValue != null) {
                l1Cache.put(key, dbValue);
            }
            return dbValue;
        } finally {
            lock.unlock();
            // Clean up lock after a delay to avoid unbounded map growth
            if (!lock.hasQueuedThreads()) {
                locks.remove(key);
            }
        }
    }

    public void evict(String key) {
        l1Cache.invalidate(key);
        if (redisTemplate != null) {
            redisTemplate.delete(key);
        }
    }

    public long l1Size() {
        return l1Cache.estimatedSize();
    }

    public String cacheStats() {
        return l1Cache.stats().toString();
    }
}
