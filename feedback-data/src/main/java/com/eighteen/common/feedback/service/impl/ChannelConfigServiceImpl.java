package com.eighteen.common.feedback.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.ChannelConfigMapper;
import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import com.eighteen.common.feedback.service.ChannelConfigService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author lcomplete
 */
@Service
@Slf4j
public class ChannelConfigServiceImpl implements ChannelConfigService {

    private Cache<String, ThrowChannelConfig> configCache = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();

    @Autowired
    ChannelConfigMapper channelConfigMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    private static String key = "#channelMap#";

    @Override
    public List<ThrowChannelConfig> getThrowChannelConfigs() {
        List<ThrowChannelConfig> list = null;
        try {
            Object obj = redisTemplate.opsForValue().get("#channelconfigscache#");
            if (obj == null) {
                list = channelConfigMapper.throwChannelConfigList();
                redisTemplate.opsForValue().set("#channelconfigscache#", JSONObject.toJSONString(list));
            } else {
                list = JSONObject.parseArray(obj.toString(), ThrowChannelConfig.class);
            }
        } catch (Exception e) {
            log.error("step get channelconfigscache error -> {}", e.getMessage());
        }
        return list;
    }

    @Override
    public ThrowChannelConfig getByChannel(String channel) {
        if (StringUtils.isBlank(channel)) return null;
        ThrowChannelConfig config = configCache.getIfPresent(channel);
        if (config != null) {
            return config;
        }
        if (config == null) {
            config = (ThrowChannelConfig) redisTemplate.opsForHash().get(key, channel);
        }
        if (config == null) {
            config = channelConfigMapper.getOne(channel);
            Optional.ofNullable(config).ifPresent(throwChannelConfig -> {
                redisTemplate.opsForHash().put(key, channel, throwChannelConfig);
            });
        }
        if (config != null) {
            configCache.put(channel, config);
        }
        return config;
    }
}
