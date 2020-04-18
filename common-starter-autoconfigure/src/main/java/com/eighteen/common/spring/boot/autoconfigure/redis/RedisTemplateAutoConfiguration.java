package com.eighteen.common.spring.boot.autoconfigure.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnClass(RedisOperations.class)
public class RedisTemplateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name="redisTemplate")
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate template=new RedisTemplate();
        RedisTemplateHelper.setSerializer(template);
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
