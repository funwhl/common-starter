package com.eighteen.common.spring.boot.autoconfigure.pika;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * Helper class that simplifies pika data access code (same as redisTemplate)
 * @author lcomplete
 */
public class PikaTemplate<K,V> extends RedisTemplate<K,V> {
}
