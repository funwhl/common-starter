package com.eighteen.common.feedback.service.impl;

import com.alibaba.fastjson.JSON;
import com.eighteen.common.feedback.constants.Constants;
import com.eighteen.common.feedback.dao.FeedbackConfigMapper;
import com.eighteen.common.feedback.domain.FeedbackConfig;
import com.eighteen.common.feedback.service.FeedbackConfigService;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.eighteen.common.feedback.constants.Constants.RedisKeys.*;

/**
 * @author : wangwei
 * @date : 2020/4/25 13:31
 */
@Service
public class FeedbackConfigServiceImpl implements FeedbackConfigService {
    @Autowired
    FeedbackConfigMapper feedbackConfigMapper;
    @Autowired
    RedisTemplate redisTemplate;
    private Cache<String, Map<String, String>> configWdsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    private Cache<String, List<String>> configChannelsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    @Override
    public List<String> getMatchFields(String channel) {
        Map<String, String> map = Optional.ofNullable(configWdsCache.getIfPresent(FEED_BACK_CONFIG_WDS)).orElse(new HashMap<>());
        String value = Optional.ofNullable(map.get(channel)).orElseGet(() -> {
            String wds = (String) redisTemplate.opsForHash().get(FEED_BACK_CONFIG_WDS, channel);
            if (StringUtils.isNotBlank(wds)) {
                map.put(channel, wds);
                configWdsCache.put(FEED_BACK_CONFIG_WDS, map);
                return wds;
            }
            return null;
        });
        if (value == null) {
            return null;
        }
        return Stream.of(value.split(",")).map(s -> Constants.FeedbackMatchFields.getDesc(Integer.valueOf(s))).collect(Collectors.toList());
    }

    @Override
    public List<String> getExcludeChannels() {
        return getFilterChannels(FEED_BACK_CONFIG_EXCLUDE_CHANNELS);
    }

    @Override
    public List<String> getIncludeChannels() {
        return getFilterChannels(FEED_BACK_CONFIG_INCLUDE_CHANNELS);
    }

    private List<String> getFilterChannels(String key) {
        return Optional.ofNullable(configChannelsCache.getIfPresent(key)).orElseGet(() -> {
            String value = (String) redisTemplate.opsForValue().get(key);
            List<String> collect = value == null ? new ArrayList<>() : Stream.of(value.split(",")).collect(Collectors.toList());
            configChannelsCache.put(key, collect);
            return collect;
        });
    }

    @Override
    public void refreshCache(String key) {
        FeedbackConfig feedbackConfig;
        switch (key) {
            case FEED_BACK_CONFIG_WDS:
                configWdsCache.invalidateAll();
                feedbackConfig = feedbackConfigMapper.selectOne(new FeedbackConfig().setType(Constants.FeedbackConfigType.CHANNEL_WD));
                Map<String, String> data = (Map) JSON.parse(feedbackConfig.getValue());
                if (!CollectionUtils.isEmpty(data)) {
                    redisTemplate.opsForHash().putAll(Constants.RedisKeys.FEED_BACK_CONFIG_WDS, data);
                }
                break;
            case FEED_BACK_CONFIG_EXCLUDE_CHANNELS:
            case FEED_BACK_CONFIG_INCLUDE_CHANNELS:
                configChannelsCache.invalidate(key);
                Integer type = key.equals(FEED_BACK_CONFIG_INCLUDE_CHANNELS) ? Constants.FeedbackConfigType.CHANNEL_INCLUDE : Constants.FeedbackConfigType.CHANNEL_EXCLUDE;
                feedbackConfig = feedbackConfigMapper.selectOne(new FeedbackConfig().setType(type));
                if (feedbackConfig != null && StringUtils.isNotBlank(feedbackConfig.getValue())) {
                    redisTemplate.opsForValue().set(key, feedbackConfig.getValue());
                }
                break;
        }
    }

}
