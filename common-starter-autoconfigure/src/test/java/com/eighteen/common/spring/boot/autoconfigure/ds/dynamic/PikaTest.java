package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class PikaTest {
    @Autowired
    PikaTemplate pikaTemplate;

    @Autowired
    PikaTemplate<String,DynamicDataSourceEntity> entityTemplate;

    @Autowired
    RedisTemplate redisTemplate;

    @Before
    public void post() {
        log.info(pikaTemplate.toString());
        log.info(redisTemplate.toString());
    }

    @Test
    public void testPikaTemplate() {
        pikaTemplate.opsForValue().set("pika", "pika");
        pikaTemplate.opsForValue().set("long", Long.MAX_VALUE);
        Object obj = pikaTemplate.opsForValue().get("long");
        log.info(obj.toString());
        Long value = (Long) obj;
        log.info(value.toString());
    }

    @Test
    public void testPikaSaveObject() {
        redisTemplate.opsForValue().set("entity", new DynamicDataSourceEntity("type"));
        Object obj = redisTemplate.opsForValue().get("entity");
        log.info(obj.toString());
    }
}
