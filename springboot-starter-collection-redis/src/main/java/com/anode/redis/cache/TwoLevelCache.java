package com.anode.redis.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.NonNull;

import java.util.concurrent.Callable;

/**
 * Two-level cache implementation that delegates to level 1 (local) and level 2 (distributed) caches.
 * Reads check level 1 first, then level 2 if not found, updating level 1 on level 2 hits.
 * Writes go to both levels.
 */
public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final Cache level1Cache;
    private final Cache level2Cache;

    public TwoLevelCache(String name, Cache level1Cache, Cache level2Cache) {
        super(true);
        this.name = name;
        this.level1Cache = level1Cache;
        this.level2Cache = level2Cache;
    }

    @Override
    protected Object lookup(@NonNull Object key) {
        // Check level 1 cache first
        var value1 = getUnwrappedValue(level1Cache, key);
        if (value1 != null) {
            return value1;
        }

        // If not in level 1, check level 2 cache
        var value2 = getUnwrappedValue(level2Cache, key);
        if (value2 != null) {
            // Update level 1 cache with value found in level 2
            level1Cache.put(key, value2);
        }
        return value2;
    }

    private Object getUnwrappedValue(Cache cache, Object key) {
        var wrapped = cache.get(key);
        if (wrapped != null) {
            return wrapped.get();
        }
        return null;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }

        try {
            T newValue = valueLoader.call();
            put(key, newValue);
            return newValue;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        level1Cache.put(key, value);
        level2Cache.put(key, value);
    }

    @Override
    public void evict(@NonNull Object key) {
        level1Cache.evict(key);
        level2Cache.evict(key);
    }

    @Override
    public void clear() {
        level1Cache.clear();
        level2Cache.clear();
    }
}
