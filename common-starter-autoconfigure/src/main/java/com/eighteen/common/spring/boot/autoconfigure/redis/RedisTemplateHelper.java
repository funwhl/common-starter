package com.eighteen.common.spring.boot.autoconfigure.redis;

import com.alibaba.fastjson.parser.ParserConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisTemplateHelper {
    public static void setSerializer(RedisTemplate redisTemplate){
        //使用fastjson序列化
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(Object.class);
        //开启全局自动类型
        ParserConfig.getGlobalInstance().addAccept("com.eighteen.");
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        // key的序列化采用StringRedisSerializer
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    }
}
