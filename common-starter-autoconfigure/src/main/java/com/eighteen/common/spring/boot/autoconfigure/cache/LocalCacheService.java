package com.eighteen.common.spring.boot.autoconfigure.cache;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 0:57
 */

import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LocalCacheService extends AbstractCacheService {
    public static final int DEFAULT_EXPIRE = 5;

    public LocalCacheService(String applicationName) {
        super(applicationName);
    }

    protected Cache doCreate(String prefix, int expire, int timeout) {
        return new LocalCache(prefix, expire);
    }

    static class LocalCache implements Cache {
        private String prefix = null;
        private int expire = -1;
        private com.google.common.cache.Cache<String, Object> cache = null;

        public LocalCache(String prefix, int expire) {
            this.prefix = prefix;
            this.expire = expire;
            this.cache = CacheBuilder.newBuilder().expireAfterWrite((long) this.expire, TimeUnit.SECONDS).build();
        }

        public <T> T get(String key, Class<T> clazz) {
            return (T) this.cache.getIfPresent(LocalCacheService.join(this.prefix, key));
        }

        public <T> T get(String key, Supplier<T> supplier) {
            String s = LocalCacheService.join(this.prefix, key);
            Object o = this.cache.getIfPresent(s);
            if (o == null) {
                o = supplier.get();
                if (o != null) {
                    this.cache.put(s, o);
                }
            }

            return (T) o;
        }

        public <T> T get(String key, Supplier<T> supplier, int expire) {
            return this.get(key, supplier);
        }

        public <T> void set(String key, T t) {
            this.cache.put(key, t);
        }

        public <T> void set(String key, T t, int expire) {
            this.set(key, t);
        }

        public void clear(String key) {
            this.cache.invalidate(LocalCacheService.join(this.prefix, key));
        }

        public void clearByPattern(String keyPattern) {
            this.cache.invalidateAll((Iterable) this.cache.asMap().keySet().stream().filter((key) -> {
                return key.contains(LocalCacheService.join(this.prefix, keyPattern));
            }).collect(Collectors.toList()));
        }

        public Long increment(String type) {
            return 0L;
        }

        public Long increment(String type, Long value) {
            return 0L;
        }

        public Long expire(String key, int second) {
            return 0L;
        }
    }
}

