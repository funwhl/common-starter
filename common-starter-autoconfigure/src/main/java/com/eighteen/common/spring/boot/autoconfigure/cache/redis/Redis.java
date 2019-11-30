package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 0:59
 */

import redis.clients.jedis.JedisCommands;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Redis {
    <T> T process(Function<JedisCommands, T> var1);

    String setex(byte[] var1, int var2, byte[] var3);

    String set(byte[] var1, byte[] var2);

    byte[] get(byte[] var1);

    Long del(String var1);

    void delByPattern(String var1);

    Long incr(String var1);

    Long incrBy(String var1, Long var2);

    Long expire(String var1, int var2);

    long sadd(String var1, String... var2);

    Set<String> smembers(String var1);

    Long scard(String var1);

    Long srem(String var1, String... var2);

    Long eval(String var1, List<String> var2, List<String> var3);

    List<String> keys(String var1, boolean var2, boolean var3);

    Long zadd(String key, Double score, String member);

    Long zexpire(String key, Double start, Double end);

    Set<String> zrange(String key, Double start, Double end);

    Set<String> zrange(String key, Double start, Double end, Supplier<Set<String>> supplier);
}
