package com.eighteen.common.feedback.test;

import com.eighteen.common.feedback.data.FeedbackRedisManager;
import com.eighteen.common.feedback.data.impl.FeedbackRedisManagerImpl;
import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import com.eighteen.common.spring.boot.autoconfigure.redis.RedisTemplateHelper;
import lombok.var;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
@SpringBootConfiguration
public class TestRedisManagerConfiguration {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RedisServer redisServer() throws IOException {
        RedisServer redisServer = new RedisServer(16379);
        return redisServer;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisServer redisServer) {
        var connectionFactory = new LettuceConnectionFactory();
        connectionFactory.setHostName("localhost");
        connectionFactory.setPort(16379);
        return connectionFactory;
    }

    @Bean
    @Primary
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate template = new RedisTemplate();
        RedisTemplateHelper.setSerializer(template);
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public PikaTemplate pikaTemplate(RedisConnectionFactory redisConnectionFactory) {
        PikaTemplate pikaTemplate = new PikaTemplate();
        RedisTemplateHelper.setSerializer(pikaTemplate);
        pikaTemplate.setConnectionFactory(redisConnectionFactory);
        return pikaTemplate;
    }

}
