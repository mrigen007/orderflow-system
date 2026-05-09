package com.orderflow.orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheManager cacheManager;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        log.info("Fetching cache statistics");

        Map<String, Object> stats = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();

        stats.put("cacheNames", cacheNames);
        stats.put("totalCaches", cacheNames.size());

        Map<String, Object> cacheDetails = new HashMap<>();
        for (String cacheName : cacheNames) {
            Map<String, Object> details = new HashMap<>();

            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                details.put("name", cacheName);
                details.put("type", cache.getClass().getSimpleName());

                // Try to get native cache for more details
                if (cache instanceof RedisCache redisCache) {
                    details.put("nativeCache", "Redis");
                }
            }

            cacheDetails.put(cacheName, details);
        }

        stats.put("caches", cacheDetails);

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/evict/{cacheName}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String cacheName) {
        log.info("Evicting cache: {}", cacheName);

        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cache '" + cacheName + "' evicted successfully"
            ));
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/evict-all")
    public ResponseEntity<Map<String, Object>> evictAllCaches() {
        log.info("Evicting all caches");

        Collection<String> cacheNames = cacheManager.getCacheNames();
        int count = 0;

        for (String cacheName : cacheNames) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                count++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All caches evicted",
                "cachesEvicted", count
        ));
    }
}