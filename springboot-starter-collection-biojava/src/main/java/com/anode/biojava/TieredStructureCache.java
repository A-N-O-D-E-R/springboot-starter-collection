package com.anode.biojava;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.biojava.nbio.structure.Structure;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class TieredStructureCache {

    private final Cache<String, Structure> l1;
    private final RedisTemplate<String, Structure> redis;
    private final B2StructureProvider b2;

    public TieredStructureCache(RedisTemplate<String, Structure> redis, B2StructureProvider b2, int l1MaxSize) {
        this.l1 = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(Duration.ofHours(1))
                .build();
        this.redis = redis;
        this.b2 = b2;
    }

    public Structure get(String pdbId) throws Exception {
        String key = pdbId.toLowerCase();

        Structure s = l1.getIfPresent(key);
        if (s != null) return s;

        if (redis != null) {
            s = redis.opsForValue().get("structure:" + key);
            if (s != null) {
                l1.put(key, s);
                return s;
            }
        }

        if (b2 != null) {
            s = b2.getStructure(key);
            if (s != null) {
                l1.put(key, s);
                if (redis != null) {
                    redis.opsForValue().set("structure:" + key, s, Duration.ofHours(24));
                }
                return s;
            }
        }

        return null;
    }

    public void put(String pdbId, Structure structure) throws Exception {
        String key = pdbId.toLowerCase();
        l1.put(key, structure);

        if (redis != null) {
            redis.opsForValue().set("structure:" + key, structure, Duration.ofHours(24));
        }
    }

    public void invalidate(String pdbId) {
        String key = pdbId.toLowerCase();
        l1.invalidate(key);
        if (redis != null) {
            redis.delete("structure:" + key);
        }
    }
}
