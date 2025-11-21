package com.anode.redis.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-level cache manager that combines a level 1 (local/in-memory) cache
 * with a level 2 (distributed/Redis) cache.
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager level1CacheManager;
    private final CacheManager level2CacheManager;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

    public TwoLevelCacheManager(CacheManager level1CacheManager, CacheManager level2CacheManager) {
        this.level1CacheManager = level1CacheManager;
        this.level2CacheManager = level2CacheManager;
    }

    public Cache getCache(@NonNull String name) {
        return cacheMap.computeIfAbsent(name, this::createTwoLevelCache);
    }

    private Cache createTwoLevelCache(String name) {
        return new TwoLevelCache(name, level1CacheManager.getCache(name), level2CacheManager.getCache(name));
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }
}
