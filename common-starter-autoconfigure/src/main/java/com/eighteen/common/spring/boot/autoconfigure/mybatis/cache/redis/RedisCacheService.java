package com.eighteen.common.spring.boot.autoconfigure.mybatis.cache.redis;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 0:59
 */

import com.eighteen.common.spring.boot.autoconfigure.cache.AbstractCacheService;
import com.eighteen.common.spring.boot.autoconfigure.cache.Cache;
import com.eighteen.common.serializer.Serializer;

import java.util.function.Supplier;

public class RedisCacheService extends AbstractCacheService {
    private Redis redis = null;
    private Serializer serializer = null;

    public RedisCacheService(String applicationName) {
        super(applicationName);
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public void setSerializer(Serializer ser) {
        this.serializer = ser;
    }

    protected Cache doCreate(String prefix, int expire, int timeout) {
        return new RedisCache(this.redis, this.serializer, prefix, expire);
    }

    static class RedisCache implements Cache {
        private Redis redis = null;
        private Serializer serializer = null;
        private String prefix = null;
        private int expire = -1;

        public RedisCache(Redis redis, Serializer ser, String prefix, int expire) {
            this.redis = redis;
            this.serializer = ser;
            this.prefix = prefix;
            this.expire = expire;
        }

        public <T> T get(String key, Class<T> clazz) {
            byte[] bytes = this.redis.get(RedisCacheService.join(this.prefix, key).getBytes());
            return bytes == null ? null : this.read(bytes, clazz);
        }

        public <T> T get(String key, Supplier<T> supplier, int exp) {
            String s = RedisCacheService.join(this.prefix, key);
            byte[] bytes = this.redis.get(s.getBytes());
            if (bytes == null) {
                T t = supplier.get();
                if (t == null) {
                    return null;
                } else {
                    bytes = this.write(t);
                    if (exp > 0) {
                        this.redis.setex(s.getBytes(), exp, bytes);
                    } else {
                        this.redis.set(s.getBytes(), bytes);
                    }

                    return t;
                }
            } else {
                return (T) this.read(bytes, Object.class);
            }
        }

        public <T> void set(String key, T t) {
            String s = RedisCacheService.join(this.prefix, key);
            this.redis.set(s.getBytes(), this.write(t));
        }

        public <T> void set(String key, T t, int exp) {
            String s = RedisCacheService.join(this.prefix, key);
            byte[] bytes = this.write(t);
            this.redis.setex(s.getBytes(), exp, bytes);
        }

        public <T> T get(String key, Supplier<T> supplier) {
            return this.get(key, supplier, this.expire);
        }

        public void clear(String key) {
            this.redis.del(RedisCacheService.join(this.prefix, key));
        }

        public void clearByPattern(String keyPattern) {
            this.redis.delByPattern(RedisCacheService.join(this.prefix, keyPattern));
        }

        public Long increment(String type) {
            return this.redis.incr(RedisCacheService.join(this.prefix, type));
        }

        public Long increment(String type, Long value) {
            return this.redis.incrBy(RedisCacheService.join(this.prefix, type), value);
        }

        public Long expire(String key, int second) {
            return this.redis.expire(RedisCacheService.join(this.prefix, key), second);
        }

        private <T> byte[] write(T t) {
            return this.serializer.write(t);
        }

        private <T> T read(byte[] bytes, Class<T> clazz) {
            return this.serializer.read(bytes, clazz);
        }
    }
}

