package com.eighteen.common.feedback.data.impl;

import com.eighteen.common.feedback.data.FeedbackRedisManager;
import com.eighteen.common.feedback.data.RedisKeyManager;
import com.eighteen.common.feedback.domain.MatchClickLogResult;
import com.eighteen.common.feedback.domain.MatchNewUserRetryResult;
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
        String uniqueClickLogId = RedisKeyManager.getUniqueClickLogId(channelType, clickLog.getId());
        redisKeys.forEach(redisKey -> {
            doSaveClickLogId(redisKey, uniqueClickLogId);
        });
        doSaveClickLog(channelType, clickLog);
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
            redisKeys.addAll(getClickLogIdRedisKey(key, coid, ncoid, channel, isAllMatch));
        });
        return redisKeys;
    }

    private List<String> getClickLogIdRedisKey(String key, Integer coid, Integer ncoid, String channel, Boolean isAllMatch) {
        List<String> redisKeys = Lists.newArrayList();
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
     * @param channelType
     * @param clickLog
     */
    private void doSaveClickLog(String channelType, ClickLog clickLog) {
        String redisKey = RedisKeyManager.getClickLogDataKey(channelType, clickLog.getId());
        redisTemplate.opsForValue().set(redisKey, clickLog, 3, TimeUnit.HOURS);
    }

    @Override
    public MatchNewUserRetryResult matchUniqueNewUserRetryId(ClickLog clickLog, String channelType) {
        Assert.notNull(clickLog, "clickLog不能为空");

        List<String> keys = getClickLogKeys(clickLog, channelType);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, clickLog.getCoid(), clickLog.getNcoid(), clickLog.getChannel(),
                null);
        for (String redisKey : redisKeys) {
            String uniqueNewUserRetryId = getUniqueNewUserRetryId(redisKey);
            if (StringUtils.isNotBlank(uniqueNewUserRetryId)) {
                String[] newUserRetryInfoArray = uniqueNewUserRetryId.split("_");
                MatchNewUserRetryResult retryResult = new MatchNewUserRetryResult();
                retryResult.setDataSource(newUserRetryInfoArray[0]);
                retryResult.setNewUserRetryId(Long.valueOf(newUserRetryInfoArray[1]));
                return retryResult;
            }
        }
        return null;
    }

    private String getUniqueNewUserRetryId(String redisKey) {
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        Object obj = template.opsForValue().get(redisKey);
        return obj == null ? "" : obj.toString();
    }

    @Override
    public MatchClickLogResult matchUniqueClickLogId(ActiveFeedbackMatch activeFeedbackMatch) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");
        MatchClickLogResult clickLogResult = null;

        List<String> keys = getActiveMatchKeys(activeFeedbackMatch.getIimei(), activeFeedbackMatch.getImei(),
                activeFeedbackMatch.getOaid(), activeFeedbackMatch.getAndroidid());
        boolean matchedBefore = checkClickLogMatchedBefore(keys);//检查是否回传过
        if (matchedBefore) {
            clickLogResult = new MatchClickLogResult();
            clickLogResult.setMatchedBefore(true);
        } else {
            boolean isAllMatch = getIsAllMatch(activeFeedbackMatch.getChannel());
            for (String key : keys) {
                List<String> redisKeys = getClickLogIdRedisKey(key, activeFeedbackMatch.getCoid(), activeFeedbackMatch.getNcoid(),
                        activeFeedbackMatch.getChannel(), isAllMatch);
                if (CollectionUtils.isEmpty(redisKeys)) {
                    continue;
                }
                for (String redisKey : redisKeys) {
                    String uniqueClickLogId = getUniqueClickLogId(redisKey);
                    if (StringUtils.isNotBlank(uniqueClickLogId)) {
                        String[] clickLogInfoArray = uniqueClickLogId.split("_");
                        clickLogResult = new MatchClickLogResult();
                        clickLogResult.setChannelType(clickLogInfoArray[0]);
                        clickLogResult.setClickLogId(Long.valueOf(clickLogInfoArray[1]));
                        clickLogResult.setMatchKey(key);
                        return clickLogResult;
                    }
                }
            }
        }
        return clickLogResult;
    }

    /**
     * 检查之前是否匹配回传过
     *
     * @param keys
     * @return
     */
    private boolean checkClickLogMatchedBefore(List<String> keys) {
        RedisTemplate storeTemplate = pikaTemplate != null ? pikaTemplate : redisTemplate;
        for (String key : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(key);
            if (storeTemplate.hasKey(redisKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void saveMatchedFeedbackRecord(ActiveFeedbackMatch activeFeedbackMatch) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");
        List<String> keys = getActiveMatchKeys(activeFeedbackMatch.getIimei(), activeFeedbackMatch.getImei(),
                activeFeedbackMatch.getOaid(), activeFeedbackMatch.getAndroidid());
        doSaveMatchedFeedbackRecord(keys);
    }

    private void doSaveMatchedFeedbackRecord(List<String> keys) {
        RedisTemplate storeTemplate = pikaTemplate != null ? pikaTemplate : redisTemplate;
        for (String key : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(key);
            storeTemplate.opsForValue().set(redisKey, 1);
        }
    }

    @Override
    public ClickLog getClickLog(String channelType, Long clickLogId) {
        Object obj = redisTemplate.opsForValue().get(RedisKeyManager.getClickLogDataKey(channelType, clickLogId));
        return obj == null ? null : (ClickLog) obj;
    }

    private String getUniqueClickLogId(String redisKey) {
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        Object obj = template.opsForValue().get(redisKey);
        return obj == null ? "" : obj.toString();
    }

    @Override
    public void saveNewUserRetry(NewUserRetry newUserRetry) {
        if (newUserRetry == null || newUserRetry.getId() == 0) {
            throw new IllegalArgumentException("newUserRetry必须持久化有id");
        }

        boolean isAllMatch = getIsAllMatch(newUserRetry.getChannel());
        List<String> keys = getActiveMatchKeys(newUserRetry.getIimei(), newUserRetry.getImei(),
                newUserRetry.getOaid(), newUserRetry.getAndroidid());
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), isAllMatch);
        String uniqueUserRetryId = RedisKeyManager.getUniqueUserRetryId(newUserRetry.getDataSource(), newUserRetry.getId());
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
