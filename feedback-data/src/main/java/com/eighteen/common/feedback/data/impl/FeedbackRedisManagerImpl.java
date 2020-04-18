package com.eighteen.common.feedback.data.impl;

import com.eighteen.common.feedback.data.FeedbackRedisManager;
import com.eighteen.common.feedback.data.RedisKeyManager;
import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.NewUserRetry;
import com.eighteen.common.feedback.service.ChannelConfigService;
import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import com.eighteen.common.utils.DigestUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 回传redis数据管理
 *
 * @author lcomplete
 */
@Component
public class FeedbackRedisManagerImpl implements FeedbackRedisManager {

    private interface ChannelType {
        String BAIDU_CHANNEL = "baiduChannel";
    }

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired(required = false)
    PikaTemplate pikaTemplate;

    @Autowired
    ChannelConfigService channelConfigService;

    @Override
    public void saveClickLog(ClickLog clickLog, String channelType) {
        Assert.notNull(clickLog, "clickLog不能为空");
        Assert.notNull(channelType, "channelType不能为空");

        List<String> keys = getClickLogKeys(clickLog, channelType);
        List<String> redisKeys = getClickLogIdRedisKeys(keys, clickLog.getCoid(), clickLog.getNcoid(),
                clickLog.getChannel(), null);
        String uniqueClickLogId = String.format("%s_%d", channelType, clickLog.getId());
        redisKeys.forEach(redisKey -> {
            doSaveClickLogId(redisKey, uniqueClickLogId);
        });
        doSaveClickLog(uniqueClickLogId, clickLog);
    }

    private List<String> getClickLogKeys(ClickLog clickLog, String channelType) {
        List<String> keys = Lists.newArrayList(clickLog.getImei(), clickLog.getOaid(), clickLog.getAndroidId());
        if (ChannelType.BAIDU_CHANNEL.equals(channelType)) {
            keys.add(clickLog.getIpua());
        }
        return keys;
    }

    /**
     * 获取点击日志Id的Redis相关的Key
     *
     * @param keys
     * @param coid
     * @param ncoid
     * @param channel
     * @param isAllMatch 为true时返回coid、ncoid的key，false时返回channel的key，null时都返回
     * @return
     */
    private List<String> getClickLogIdRedisKeys(List<String> keys, Integer coid, Integer ncoid, String channel, Boolean isAllMatch) {
        List<String> redisKeys = Lists.newArrayList();
        keys.forEach(key -> {
            if (StringUtils.isNotBlank(key)) {
                String coidKey = RedisKeyManager.getClickLogIdKey(key, coid, ncoid);
                String channelKey = RedisKeyManager.getClickLogIdKey(key, channel);
                if (isAllMatch == null) {
                    redisKeys.add(coidKey);
                    redisKeys.add(channelKey);
                } else if (isAllMatch) {
                    redisKeys.add(coidKey);
                } else {
                    redisKeys.add(channelKey);
                }
            }
        });
        return redisKeys;
    }

    /**
     * 保存点击日志Id
     *
     * @param redisKey
     * @param id
     */
    private void doSaveClickLogId(String redisKey, String id) {
        //如果使用pika，则redis中只保存最近3个小时的id
        if (pikaTemplate != null) {
            pikaTemplate.opsForValue().set(redisKey, id, 7, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(redisKey, id, 3, TimeUnit.HOURS);
        } else {
            redisTemplate.opsForValue().set(redisKey, id, 7, TimeUnit.DAYS);
        }
    }

    /**
     * 保存点击日志
     *
     * @param id
     * @param clickLog
     */
    private void doSaveClickLog(String id, ClickLog clickLog) {
        String redisKey = RedisKeyManager.getClickLogDataKey(id);
        redisTemplate.opsForValue().set(redisKey, clickLog, 3, TimeUnit.HOURS);
    }

    @Override
    public String matchUniqueNewUserRetryId(ClickLog clickLog, String channelType) {
        Assert.notNull(clickLog, "clickLog不能为空");

        List<String> keys = getClickLogKeys(clickLog, channelType);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, clickLog.getCoid(), clickLog.getNcoid(), clickLog.getChannel(),
                null);
        for (String redisKey : redisKeys) {
            String retryId = getUniqueNewUserRetryId(redisKey);
            if (StringUtils.isNotBlank(retryId)) {
                return retryId;
            }
        }
        return "";
    }

    private String getUniqueNewUserRetryId(String redisKey) {
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        Object obj = template.opsForValue().get(redisKey);
        return obj == null ? "" : obj.toString();
    }

    @Override
    public String matchUniqueClickLogId(ActiveFeedbackMatch activeFeedbackMatch) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");

        boolean isAllMatch = getIsAllMatch(activeFeedbackMatch.getChannel());
        List<String> keys = getActiveMatchKeys(activeFeedbackMatch.getIimei(), activeFeedbackMatch.getImei(),
                activeFeedbackMatch.getOaid(), activeFeedbackMatch.getAndroidid());
        List<String> redisKeys = getClickLogIdRedisKeys(keys, activeFeedbackMatch.getCoid(), activeFeedbackMatch.getNcoid(),
                activeFeedbackMatch.getChannel(), isAllMatch);
        for (String redisKey : redisKeys) {
            String clickLogId = getUniqueClickLogId(redisKey);
            if (StringUtils.isNotBlank(clickLogId)) {
                return clickLogId;
            }
        }
        return "";
    }

    @Override
    public ClickLog getClickLog(String id) {
        Object obj = redisTemplate.opsForValue().get(RedisKeyManager.getClickLogDataKey(id));
        return obj == null ? null : (ClickLog) obj;
    }

    private String getUniqueClickLogId(String redisKey) {
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        Object obj = template.opsForValue().get(redisKey);
        return obj == null ? "" : obj.toString();
    }

    @Override
    public void saveNewUserRetry(NewUserRetry newUserRetry) {
        if (newUserRetry == null && newUserRetry.getId() > 0) {
            return;
        }

        boolean isAllMatch = getIsAllMatch(newUserRetry.getChannel());
        List<String> keys = getActiveMatchKeys(newUserRetry.getIimei(), newUserRetry.getImei(),
                newUserRetry.getOaid(), newUserRetry.getAndroidid());
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), isAllMatch);
        String uniqueUserRetryId=String.format("%s_%d",newUserRetry.getDataSource(),newUserRetry.getId());
        redisKeys.forEach(redisKey -> {
            doSaveNewUserRetryId(redisKey, uniqueUserRetryId);
        });
    }

    private boolean getIsAllMatch(String channel) {
        List<ThrowChannelConfig> channelConfigs = channelConfigService.getThrowChannelConfigs();
        AtomicBoolean isAllMatch = new AtomicBoolean(false);
        if (!CollectionUtils.isEmpty(channelConfigs)) {
            channelConfigs.stream().filter(config -> config.getChannel().equals(channel)).findAny().ifPresent(
                    config -> isAllMatch.set(config.getChannelType() == 0)
            );
        }
        return isAllMatch.get();
    }

    private static List<String> excludeKeys = Lists.newArrayList(null, "null", "Unknown", "Null", "NULL", "{{IMEI}}", "{{ANDDROID_ID}}", "{{OAID}}", "", "__IMEI__", "__OAID__");

    /**
     * 获取激活数据的key
     *
     * @param iimei
     * @param imei
     * @param oaid
     * @param androidid
     * @return
     */
    private List<String> getActiveMatchKeys(String iimei, String imei, String oaid, String androidid) {
        List<String> keys = Lists.newArrayList(oaid, androidid);
        //todo check ipua 如何加入key中
        if (StringUtils.isNotBlank(iimei)) {
            keys.addAll(Lists.newArrayList(iimei.split(",")));
        } else {
            keys.add(imei);
        }
        keys.removeAll(excludeKeys);
        keys = keys.stream().filter(k -> StringUtils.isNotBlank(k)).map(k -> DigestUtils.getMd5Str(k))
                .collect(Collectors.toList());
        return keys;
    }

    /**
     * 获取激活重试Id相关的key
     *
     * @param keys
     * @param coid
     * @param ncoid
     * @param channel
     * @param isAllMatch 为true时返回coid、ncoid的key，false时返回channel的key，null时都返回
     * @return
     */
    private List<String> getNewUserRetryIdRedisKeys(List<String> keys, Integer coid, Integer ncoid, String channel, Boolean isAllMatch) {
        List<String> redisKeys = Lists.newArrayList();
        keys.forEach(key -> {
            if (StringUtils.isNotBlank(key)) {
                String coidKey = RedisKeyManager.getNewUserRetryIdKey(key, coid, ncoid);
                String channelKey = RedisKeyManager.getNewUserRetryIdKey(key, channel);
                if (isAllMatch == null) {
                    redisKeys.add(coidKey);
                    redisKeys.add(channelKey);
                } else if (isAllMatch) {
                    redisKeys.add(coidKey);
                } else {
                    redisKeys.add(channelKey);
                }
            }
        });
        return redisKeys;
    }

    private void doSaveNewUserRetryId(String redisKey, String id) {
        //启用了pika则保存在pika中
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        template.opsForValue().set(redisKey, id);
    }

}
