package com.eighteen.common.feedback.data.impl;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.data.FeedbackRedisManager;
import com.eighteen.common.feedback.data.RedisKeyManager;
import com.eighteen.common.feedback.domain.ActiveMatchKeyField;
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
import org.springframework.beans.BeanUtils;
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

    private interface ClickType {
        String BAIDU_CHANNEL = "baiduChannel";
    }

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired(required = false)
    PikaTemplate pikaTemplate;

    @Autowired
    ChannelConfigService channelConfigService;

    @Override
    public void saveClickLog(ClickLog clickLog, String clickType) {
        Assert.notNull(clickLog, "clickLog不能为空");
        Assert.notNull(clickType, "clickType不能为空");

        List<String> keys = getClickLogKeys(clickLog, clickType);
        List<String> redisKeys = getClickLogIdRedisKeys(keys, clickLog.getCoid(), clickLog.getNcoid(),
                clickLog.getChannel(), null);
        String uniqueClickLogId = RedisKeyManager.getUniqueClickLogId(clickType, clickLog.getId());
        redisKeys.forEach(redisKey -> {
            doSaveClickLogId(redisKey, uniqueClickLogId);
        });
        doSaveClickLog(clickType, clickLog);
    }

    private List<String> getClickLogKeys(ClickLog clickLog, String clickType) {
        List<String> keys = Lists.newArrayList(clickLog.getImei(), clickLog.getOaid(), clickLog.getAndroidId());
        if (ClickType.BAIDU_CHANNEL.equals(clickType)) {
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
     * @param clickType
     * @param clickLog
     */
    private void doSaveClickLog(String clickType, ClickLog clickLog) {
        String redisKey = RedisKeyManager.getClickLogDataKey(clickType, clickLog.getId());
        redisTemplate.opsForValue().set(redisKey, clickLog, 3, TimeUnit.HOURS);
    }

    @Override
    public MatchNewUserRetryResult matchNewUserRetry(ClickLog clickLog, String clickType) {
        Assert.notNull(clickLog, "clickLog不能为空");

        List<String> keys = getClickLogKeys(clickLog, clickType);
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
    public MatchClickLogResult matchClickLog(ActiveFeedbackMatch activeFeedbackMatch) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");
        MatchClickLogResult clickLogResult = null;

        List<ActiveMatchKeyField> keyFields = getActiveMatchKeyFields(activeFeedbackMatch);
        List<String> keys = keyFields.stream().map(kf -> kf.getMatchKey()).collect(Collectors.toList());
        boolean matchedBefore = checkClickLogMatchedBefore(keys);//检查是否回传过
        if (matchedBefore) {
            clickLogResult = new MatchClickLogResult();
            clickLogResult.setMatchedBefore(true);
        } else {
            boolean isAllMatch = getIsAllMatch(activeFeedbackMatch.getChannel());
            for (ActiveMatchKeyField keyField : keyFields) {
                List<String> redisKeys = getClickLogIdRedisKey(keyField.getMatchKey(), activeFeedbackMatch.getCoid(), activeFeedbackMatch.getNcoid(),
                        activeFeedbackMatch.getChannel(), isAllMatch);
                if (CollectionUtils.isEmpty(redisKeys)) {
                    continue;
                }
                for (String redisKey : redisKeys) {
                    String uniqueClickLogId = getUniqueClickLogId(redisKey);
                    if (StringUtils.isNotBlank(uniqueClickLogId)) {
                        String[] clickLogInfoArray = uniqueClickLogId.split("_");
                        clickLogResult = new MatchClickLogResult();
                        clickLogResult.setClickType(clickLogInfoArray[0]);
                        clickLogResult.setClickLogId(Long.valueOf(clickLogInfoArray[1]));
                        clickLogResult.setMatchKey(keyField.getMatchKey());
                        clickLogResult.setMatchField(keyField.getMatchField());
                        return clickLogResult;
                    }
                }
            }
        }
        return clickLogResult;
    }

    private List<ActiveMatchKeyField> getActiveMatchKeyFields(ActiveFeedbackMatch feedbackMatch) {
        String iimei = feedbackMatch.getIimei();
        String imei = feedbackMatch.getImei();
        String oaid = feedbackMatch.getOaid();
        String androidid = feedbackMatch.getAndroidid();
        List<ActiveMatchKeyField> keys = Lists.newArrayList(
                new ActiveMatchKeyField("oaid", oaid),
                new ActiveMatchKeyField("androidId", androidid)
        );
        //todo check ipua 完善判断
        if (feedbackMatch.getChannel() == "baiduChannelStaf") {
            keys.add(new ActiveMatchKeyField("ipua", feedbackMatch.getIp() + "#" + feedbackMatch.getUa()));
        }
        if (StringUtils.isNotBlank(iimei)) {
            for (String mei : iimei.split(",")) {
                keys.add(new ActiveMatchKeyField("imei", mei));
            }
        } else {
            keys.add(new ActiveMatchKeyField("imei", imei));
        }

        keys = keys.stream().filter(k -> StringUtils.isNotBlank(k.getMatchKey()))
                .filter(k -> !excludeKeys.contains(k.getMatchKey()))
                .collect(Collectors.toList());
        keys.forEach(k -> k.setMatchKey(DigestUtils.getMd5Str(k.getMatchKey())));
        return keys;
    }

    /**
     * 获取激活数据的key
     *
     * @return
     */
    private List<String> getActiveMatchKeys(ActiveFeedbackMatch feedbackMatch) {
        List<ActiveMatchKeyField> keyFields = getActiveMatchKeyFields(feedbackMatch);
        return keyFields.stream().map(kf -> kf.getMatchKey()).collect(Collectors.toList());
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
    public void saveMatchedFeedbackRecord(ActiveFeedbackMatch activeFeedbackMatch, String clickChannel) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");
        List<String> keys = getActiveMatchKeys(activeFeedbackMatch);
        doSaveMatchedFeedbackRecord(keys, clickChannel);
    }

    private void doSaveMatchedFeedbackRecord(List<String> keys, String channel) {
        RedisTemplate storeTemplate = pikaTemplate != null ? pikaTemplate : redisTemplate;
        for (String key : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(key);
            storeTemplate.opsForValue().set(redisKey, channel);
        }
    }

    @Override
    public ClickLog getClickLog(String clickType, Long clickLogId) {
        Object obj = redisTemplate.opsForValue().get(RedisKeyManager.getClickLogDataKey(clickType, clickLogId));
        if (obj != null) {
            if (obj instanceof ClickLog) {
                return (ClickLog) obj;
            } else if (obj instanceof JSONObject) {
                return ((JSONObject) obj).toJavaObject(ClickLog.class);
            }
        }
        return null;
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
        ActiveFeedbackMatch feedbackMatch = new ActiveFeedbackMatch();
        BeanUtils.copyProperties(newUserRetry, feedbackMatch);
        List<String> keys = getActiveMatchKeys(feedbackMatch);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), isAllMatch);
        String uniqueUserRetryId = RedisKeyManager.getUniqueUserRetryId(newUserRetry.getDataSource(), newUserRetry.getId());
        redisKeys.forEach(redisKey -> {
            doSaveNewUserRetryId(redisKey, uniqueUserRetryId);
        });
    }

    @Override
    public void deleteNewUserRetry(NewUserRetry newUserRetry) {
        Assert.notNull(newUserRetry, "newUserRetry cannot be null");

        boolean isAllMatch = getIsAllMatch(newUserRetry.getChannel());
        ActiveFeedbackMatch feedbackMatch = new ActiveFeedbackMatch();
        BeanUtils.copyProperties(newUserRetry, feedbackMatch);
        List<String> keys = getActiveMatchKeys(feedbackMatch);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), isAllMatch);
        redisKeys.forEach(redisKey -> {
            doDeleteNewUserRetryId(redisKey);
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
        template.opsForValue().set(redisKey, id, 1, TimeUnit.DAYS);
    }

    private void doDeleteNewUserRetryId(String redisKey) {
        //启用了pika则保存在pika中
        RedisTemplate template = pikaTemplate != null ? pikaTemplate : redisTemplate;
        template.delete(redisKey);
    }

}
