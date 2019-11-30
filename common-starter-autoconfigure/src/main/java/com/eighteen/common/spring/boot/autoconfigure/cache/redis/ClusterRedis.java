package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 0:58
 */

import redis.clients.jedis.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class ClusterRedis implements Redis {
    private JedisCluster jedisCluster;

    public ClusterRedis() {
    }

    static void close(Jedis jedis, Throwable var9) {
        if (jedis != null) {
            if (var9 != null) {
                try {
                    jedis.close();
                } catch (Throwable var20) {
                    var9.addSuppressed(var20);
                }
            } else {
                jedis.close();
            }
        }
    }

    public <T> T process(Function<JedisCommands, T> jedisTFunction) {
        return jedisTFunction.apply(this.jedisCluster);
    }

    public void setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public String setex(byte[] key, int seconds, byte[] value) {
        return this.jedisCluster.setex(key, seconds, value);
    }

    public String set(byte[] key, byte[] value) {
        return this.jedisCluster.set(key, value);
    }

    public byte[] get(byte[] key) {
        return this.jedisCluster.get(key);
    }

    public Long del(String key) {
        return this.jedisCluster.del(key);
    }

    public void delByPattern(String keyPattern) {
        Map<String, JedisPool> nodes = this.jedisCluster.getClusterNodes();
        Iterator var3 = nodes.values().iterator();

        while (var3.hasNext()) {
            JedisPool node = (JedisPool) var3.next();
            Jedis jedis = node.getResource();
            Throwable var6 = null;

            try {
                ScanParams params = new ScanParams();
                params.match(keyPattern + "*");
                String nextCursor = "0";

                while (true) {
                    ScanResult<String> result = jedis.scan(nextCursor, params);
                    List<String> keys = result.getResult();
                    nextCursor = result.getStringCursor();
                    Iterator var11 = keys.iterator();

                    while (var11.hasNext()) {
                        String key = (String) var11.next();
                        jedis.del(key);
                    }

                    if (nextCursor.equals("0")) {
                        break;
                    }
                }
            } catch (Throwable var20) {
                var6 = var20;
                throw var20;
            } finally {
                close(jedis, var6);

            }
        }

    }

    public Long incr(String key) {
        return this.jedisCluster.incr(key);
    }

    public Long incrBy(String key, Long value) {
        return this.jedisCluster.incrBy(key, value);
    }

    public Long expire(String key, int second) {
        return this.jedisCluster.expire(key, second);
    }

    public long sadd(String key, String... value) {
        return this.jedisCluster.sadd(key, value);
    }

    public Set<String> smembers(String key) {
        return this.jedisCluster.smembers(key);
    }

    public Long scard(String key) {
        return this.jedisCluster.scard(key);
    }

    public Long srem(String key, String... values) {
        return this.jedisCluster.srem(key, values);
    }

    public Long eval(String script, List<String> keys, List<String> args) {
        return (Long) this.jedisCluster.eval(script, keys, args);
    }

    public List<String> keys(String pattern, boolean matchPrefix, boolean matchSuffix) {
        List<String> data = new ArrayList<>();
        Map<String, JedisPool> nodes = this.jedisCluster.getClusterNodes();
        Iterator var6 = nodes.values().iterator();

        while (var6.hasNext()) {
            JedisPool node = (JedisPool) var6.next();
            Jedis jedis = node.getResource();
            Throwable var9 = null;

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

                while (true) {
                    ScanResult<String> result = jedis.scan(nextCursor, params);
                    List<String> keys = result.getResult();
                    nextCursor = result.getStringCursor();
                    data.addAll(keys);
                    if (nextCursor.equals("0")) {
                        break;
                    }
                }
            } catch (Throwable var21) {
                var9 = var21;
                throw var21;
            } finally {
                close(jedis, var9);

            }
        }

        return data;
    }

    @Override
    public Long zadd(String key, Double score, String member) {
        return this.jedisCluster.zadd(key, score, member);
    }


    @Override
    public Long zexpire(String key, Double start, Double end) {
        return jedisCluster.zremrangeByScore(key, start, end);
    }

    @Override
    public Set<String> zrange(String key, Double start, Double end) {
        return jedisCluster.zrangeByScore(key, start, end);
    }

    @Override
    public Set<String> zrange(String key, Double start, Double end, Supplier<Set<String>> supplier) {
        Set<String> var3 = zrange(key, start, end);
        return var3 == null || var3.size() == 0 ? supplier.get() : var3;
    }

}
