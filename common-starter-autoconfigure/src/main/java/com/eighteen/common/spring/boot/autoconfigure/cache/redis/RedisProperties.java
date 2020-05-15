package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.eighteen.common.spring.boot.autoconfigure.cache.redis.RedisProperties.PREFIX;


/**
 * Created by eighteen.
 * Date: 2019/8/24
 * Time: 19:42
 */

@ConfigurationProperties(prefix = PREFIX)
@Data
public class RedisProperties {
    public static final String PREFIX = "spring.redis";
    private Boolean cluster = false;
    private Integer timeout = 100000;
    private String password;
    private Integer connectionTimeout = 2000;
    private Integer maxAttempts = 3;
    private Integer maxTotal = 8;
    private Integer maxIdle = 8;
    private Boolean blockWhenExhausted = true;
    private Integer maxWaitMillis = -1;
    private String nodes;
}
