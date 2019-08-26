package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 1:00
 */

import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class SingleRedis implements Redis {
    private JedisPool jedisPool;

    public SingleRedis() {
    }

    public <T> T process(Function<JedisCommands, T> jedisTFunction) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        T var4;
        try {
            var4 = jedisTFunction.apply(jedis);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String setex(byte[] key, int seconds, byte[] value) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var5 = null;

        String var6;
        try {
            var6 = jedis.setex(key, seconds, value);
        } catch (Throwable var15) {
            var5 = var15;
            throw var15;
        } finally {
            ClusterRedis.close(jedis, var5);

        }

        return var6;
    }

    public String set(byte[] key, byte[] value) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        String var5;
        try {
            var5 = jedis.set(key, value);
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    public byte[] get(byte[] key) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        byte[] var4;
        try {
            var4 = jedis.get(key);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public Long del(String key) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        Long var4;
        try {
            var4 = jedis.del(key);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public void delByPattern(String keyPattern) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        try {
            ScanParams params = new ScanParams();
            params.match(keyPattern + "*");
            String nextCursor = "0";

            do {
                ScanResult<String> result = jedis.scan(nextCursor, params);
                List<String> keys = result.getResult();
                nextCursor = result.getStringCursor();
                Iterator var8 = keys.iterator();

                while (var8.hasNext()) {
                    String key = (String) var8.next();
                    jedis.del(key);
                }
            } while (!nextCursor.equals("0"));
        } catch (Throwable var17) {
            var3 = var17;
            throw var17;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

    }

    public Long incr(String key) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        Long var4;
        try {
            var4 = jedis.incr(key);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public Long incrBy(String key, Long value) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        Long var5;
        try {
            var5 = jedis.incrBy(key, value);
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    public Long expire(String key, int second) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        Long var5;
        try {
            var5 = jedis.expire(key, second);
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    public long sadd(String key, String... value) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        long var5;
        try {
            var5 = jedis.sadd(key, value);
        } catch (Throwable var15) {
            var4 = var15;
            throw var15;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    public Set<String> smembers(String key) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        Set var4;
        try {
            var4 = jedis.smembers(key);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public Long scard(String key) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;

        Long var4;
        try {
            var4 = jedis.scard(key);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }

        return var4;
    }

    public Long srem(String key, String... values) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        Long var5;
        try {
            var5 = jedis.srem(key, values);
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    public Long eval(String script, List<String> keys, List<String> args) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var5 = null;

        Long var6;
        try {
            var6 = (Long) jedis.eval(script, keys, args);
        } catch (Throwable var15) {
            var5 = var15;
            throw var15;
        } finally {
            ClusterRedis.close(jedis, var5);

        }

        return var6;
    }

    public List<String> keys(String pattern, boolean matchPrefix, boolean matchSuffix) {
        List<String> data = new ArrayList();
        Jedis jedis = this.jedisPool.getResource();
        Throwable var6 = null;

        try {
            ScanParams params = new ScanParams();
            params.match(pattern + "*");
            if (matchPrefix && !matchSuffix) {
                params.match("*" + pattern);
            }

            if (!matchPrefix && matchSuffix) {
                params.match(pattern + "*");
            }

            if (matchPrefix && matchSuffix) {
                params.match("*" + pattern + "*");
            }

            String nextCursor = "0";

            do {
                ScanResult<String> result = jedis.scan(nextCursor, params);
                List<String> keys = result.getResult();
                data.addAll(keys);
                nextCursor = result.getStringCursor();
            } while (!nextCursor.equals("0"));
        } catch (Throwable var18) {
            var6 = var18;
            throw var18;
        } finally {
            ClusterRedis.close(jedis, var6);

        }

        return data;
    }

    public Long zadd(String key, Double score, String member) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;

        long var5;
        try {
            var5 = jedis.zadd(key, score, member);
        } catch (Throwable var15) {
            var4 = var15;
            throw var15;
        } finally {
            ClusterRedis.close(jedis, var4);

        }
        return var5;
    }

    @Override
    public Long zexpire(String key, Double start, Double end) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var4 = null;
        Long var5;
        try {
            var5 = jedis.zremrangeByScore(key, start, end);
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            ClusterRedis.close(jedis, var4);

        }

        return var5;
    }

    @Override
    public Set<String> zrange(String key, Double start, Double end) {
        Jedis jedis = this.jedisPool.getResource();
        Throwable var3 = null;
        Set<String> var4;
        try {
            var4 = jedis.zrangeByScore(key, start, end);
        } catch (Throwable var13) {
            var3 = var13;
            throw var13;
        } finally {
            ClusterRedis.close(jedis, var3);

        }
        return var4;
    }

    @Override
    public Set<String> zrange(String key, Double start, Double end, Supplier<Set<String>> supplier) {
        Set<String> var3 = zrange(key, start, end);
        return var3 == null || var3.size() == 0 ? supplier.get() : var3;
    }

}

