package com.eighteen.common.spring.boot.autoconfigure.cache.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.eighteen.common.spring.boot.autoconfigure.cache.redis.RedisProperties.PREFIX;


/**
 * Created by eighteen.
 * Date: 2019/8/24
 * Time: 19:42
 */

@ConfigurationProperties(prefix = PREFIX)
public class RedisProperties {
    public static final String PREFIX = "spring.redis";
    private Boolean cluster =false;
    private Integer timeout =10000;
    private String password;
    private Integer connectionTimeout =2000;
    private Integer maxAttempts =3;
    private Integer maxTotal=8;
    private Integer maxIdle=8;
    private Boolean blockWhenExhausted =true;
    private Integer maxWaitMillis =-1;
    private String nodes;

    public Boolean getCluster() {
        return cluster;
    }

    public void setCluster(Boolean cluster) {
        this.cluster = cluster;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(Integer maxTotal) {
        this.maxTotal = maxTotal;
    }

    public Integer getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(Integer maxIdle) {
        this.maxIdle = maxIdle;
    }

    public Boolean getBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    public void setBlockWhenExhausted(Boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
    }

    public Integer getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(Integer maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

}
