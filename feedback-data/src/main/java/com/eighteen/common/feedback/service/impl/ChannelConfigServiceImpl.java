package com.eighteen.common.feedback.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.ChannelConfigMapper;
import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import com.eighteen.common.feedback.service.ChannelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author lcomplete
 */
@Service
@Slf4j
public class ChannelConfigServiceImpl implements ChannelConfigService {
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
        ThrowChannelConfig obj = (ThrowChannelConfig) redisTemplate.opsForHash().get(key, channel);
        if (obj == null) {
            obj = channelConfigMapper.getOne(channel);
            Optional.ofNullable(obj).ifPresent(throwChannelConfig -> redisTemplate.opsForHash().put(key, channel, throwChannelConfig));
        }
        return obj;
    }
}
