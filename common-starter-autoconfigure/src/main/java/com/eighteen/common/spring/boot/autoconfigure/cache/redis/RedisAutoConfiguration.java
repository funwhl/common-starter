package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.eighteen.common.serializer.hessian.HessianSerializer;
import com.eighteen.common.serializer.Serializer;
import com.eighteen.common.spring.boot.autoconfigure.cache.Cache;
import com.eighteen.common.spring.boot.autoconfigure.cache.CacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 11:16
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(prefix = RedisProperties.PREFIX, name = "nodes")
public class RedisAutoConfiguration {
    private RedisProperties properties;

    public RedisAutoConfiguration(RedisProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(properties.getMaxTotal());
        jedisPoolConfig.setMaxIdle(properties.getMaxIdle());
        jedisPoolConfig.setBlockWhenExhausted(properties.getBlockWhenExhausted());
        jedisPoolConfig.setMaxWaitMillis(properties.getMaxAttempts());
        return jedisPoolConfig;
    }

    @Bean
    public Redis redis(JedisPoolConfig jedisPoolConfig) {
        if (properties.getCluster()) {
            ClusterRedis clusterRedis = new ClusterRedis();
            clusterRedis.setJedisCluster(new JedisCluster(
                    Arrays.stream(properties.getNodes()
                            .split(",")).map(node -> new HostAndPort(node.split(":")[0], Integer.parseInt(node.split(":")[1]))).collect(Collectors.toSet()),
                    properties.getConnectionTimeout(),
                    properties.getTimeout(),
                    properties.getMaxAttempts(),
                    properties.getPassword(),
                    jedisPoolConfig)
            );
            return clusterRedis;
        } else {
            SingleRedis singleRedis = new SingleRedis();
            String hostAndPort = properties.getNodes().split(",")[0];
            singleRedis.setJedisPool(new JedisPool(
                    jedisPoolConfig,
                    hostAndPort.split(":")[0],
                    Integer.parseInt(hostAndPort.split(":")[1]),
                    properties.getTimeout(),
                    properties.getPassword()));
            return singleRedis;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public Serializer serializer() {
        return new HessianSerializer();
    }

    @Bean
    public CacheService cacheService(Environment env, Serializer serializer, Redis redis) {
        RedisCacheService redisCacheService = new RedisCacheService(env.getProperty("spring.application.name"));
        redisCacheService.setSerializer(serializer);
        redisCacheService.setRedis(redis);
        return redisCacheService;
    }

    @Bean
    public Cache cache(Environment env, CacheService cacheService) {
        return cacheService.create(env.getProperty("spring.application.name"));
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @ConditionalOnClass(RedisOperations.class)

    public RedisTemplate<Object, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        //使用fastjson序列化
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(Object.class);
        // value值的序列化采用fastJsonRedisSerializer
        template.setValueSerializer(fastJsonRedisSerializer);
        template.setHashValueSerializer(fastJsonRedisSerializer);
        // key的序列化采用StringRedisSerializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    @ConditionalOnClass(RedisOperations.class)

    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

}
