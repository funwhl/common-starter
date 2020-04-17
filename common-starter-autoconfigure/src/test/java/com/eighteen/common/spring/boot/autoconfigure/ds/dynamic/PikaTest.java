package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import lombok.extern.slf4j.Slf4j;
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
    RedisTemplate redisTemplate;

    @Before
    public void post(){
        log.info(pikaTemplate.toString());
        log.info(redisTemplate.toString());
    }

    @Test
    public void testPikaTemplate(){
        pikaTemplate.opsForValue().set("pika","pika");
        redisTemplate.opsForValue().set("redis","redis");
    }
}
