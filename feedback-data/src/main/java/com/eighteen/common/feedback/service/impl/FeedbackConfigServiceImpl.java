package com.eighteen.common.feedback.service.impl;

import com.alibaba.fastjson.JSON;
import com.eighteen.common.feedback.constants.Constants;
import com.eighteen.common.feedback.dao.FeedbackConfigMapper;
import com.eighteen.common.feedback.domain.FeedbackConfig;
import com.eighteen.common.feedback.service.FeedbackConfigService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
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
public class FeedbackConfigServiceImpl implements FeedbackConfigService, InitializingBean {
    @Autowired
    FeedbackConfigMapper feedbackConfigMapper;
    @Autowired
    RedisTemplate redisTemplate;
    private Cache<String, Map<String, String>> mapCache = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    private Cache<String, List<String>> listCache = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    @Override
    public List<String> getMatchFields(String channel) {
        Map<String, String> map = Optional.ofNullable(mapCache.getIfPresent(FEED_BACK_CONFIG_WDS)).orElse(new HashMap<>());
        String value = Optional.ofNullable(map.get(channel)).orElseGet(() -> {
            String wds = (String) redisTemplate.opsForHash().get(FEED_BACK_CONFIG_WDS, channel);
            if (StringUtils.isNotBlank(wds)) {
                map.put(channel, wds);
                mapCache.put(FEED_BACK_CONFIG_WDS, map);
                return wds;
            }
            return null;
        });
        return value==null?null:Stream.of(value.split(",")).map(s -> Constants.FeedbackMatchFields.getDesc(Integer.valueOf(s))).collect(Collectors.toList());
    }

    @Override
    public List<String> getExcludeChannels() {
        return getCacheList(FEED_BACK_CONFIG_EXCLUDE_CHANNELS);
    }

    @Override
    public List<String> getIncludeChannels() {
        return getCacheList(FEED_BACK_CONFIG_INCLUDE_CHANNELS);
    }

    @Override
    public List<String> getIncludeTypes() {
        return getCacheList(FEED_BACK_CONFIG_INCLUDE_TYPE);
    }

    private List<String> getCacheList(String key) {
        return Optional.ofNullable(listCache.getIfPresent(key)).orElseGet(() -> {
            String value = (String) redisTemplate.opsForValue().get(key);
            List<String> collect = value == null ? new ArrayList<>() : Stream.of(value.split(",")).collect(Collectors.toList());
            listCache.put(key, collect);
            return collect;
        });
    }

    @Override
    public void refreshCache(String key) {
        FeedbackConfig feedbackConfig;
        redisTemplate.delete(key);
        switch (key) {
            case FEED_BACK_CONFIG_WDS:
                mapCache.invalidateAll();
                feedbackConfig = feedbackConfigMapper.selectOne(new FeedbackConfig().setType(Constants.FeedbackConfigType.CHANNEL_WD));
                Map<String, String> data = (Map) JSON.parse(feedbackConfig.getValue());
                if (!CollectionUtils.isEmpty(data)) {
                    redisTemplate.opsForHash().putAll(key, data);
                }
                break;
            case FEED_BACK_CONFIG_EXCLUDE_CHANNELS:
            case FEED_BACK_CONFIG_INCLUDE_CHANNELS:
            case FEED_BACK_CONFIG_INCLUDE_TYPE:
                listCache.invalidate(key);
                Integer type = key.equals(FEED_BACK_CONFIG_INCLUDE_CHANNELS) ? Constants.FeedbackConfigType.CHANNEL_INCLUDE : Constants.FeedbackConfigType.CHANNEL_EXCLUDE;
                if (key.equals(FEED_BACK_CONFIG_INCLUDE_TYPE))
                    type = Constants.FeedbackConfigType.FEEDBACK_TYPE_INCLUDE;
                feedbackConfig = feedbackConfigMapper.selectOne(new FeedbackConfig().setType(type));
                if (feedbackConfig != null && StringUtils.isNotBlank(feedbackConfig.getValue())) {
                    redisTemplate.opsForValue().set(key, feedbackConfig.getValue());
                }
                break;
        }
    }

    @Override
    public void afterPropertiesSet() {
        refreshCache(FEED_BACK_CONFIG_WDS);
        refreshCache(FEED_BACK_CONFIG_EXCLUDE_CHANNELS);
        refreshCache(FEED_BACK_CONFIG_INCLUDE_CHANNELS);
    }
}
