package com.anode.redis.cache;

import com.esotericsoftware.kryo.KryoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.lang.NonNull;

/**
 * Cache error handler that handles Kryo serialization exceptions specially.
 * When a KryoException occurs on cache get, the key is evicted to prevent repeated errors.
 */
public class KryoAwareCacheErrorHandler extends SimpleCacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(KryoAwareCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        if (exception instanceof KryoException) {
            cache.evict(key);
            log.warn("KryoException caught in KryoAwareCacheErrorHandler, key evicted", exception);
        } else {
            super.handleCacheGetError(exception, cache, key);
        }
    }
}
