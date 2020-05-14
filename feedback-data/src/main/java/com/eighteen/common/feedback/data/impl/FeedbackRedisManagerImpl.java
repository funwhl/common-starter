package com.eighteen.common.feedback.data.impl;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.constants.DsConstants;
import com.eighteen.common.feedback.data.FeedbackRedisManager;
import com.eighteen.common.feedback.data.HashKeyFields;
import com.eighteen.common.feedback.data.RedisKeyManager;
import com.eighteen.common.feedback.datasource.DataSourcePicker;
import com.eighteen.common.feedback.domain.*;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.NewUserRetry;
import com.eighteen.common.feedback.service.ChannelConfigService;
import com.eighteen.common.feedback.service.FeedbackConfigService;
import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import com.eighteen.common.utils.DigestUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.swing.text.html.Option;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.eighteen.common.feedback.constants.Constants.EventType.ACTIVE;

/**
 * 回传redis数据管理
 *
 * @author lcomplete
 */
@Component
@Slf4j
public class FeedbackRedisManagerImpl implements FeedbackRedisManager {
    private static List<String> excludeKeys = Lists.newArrayList(null, "null", "Unknown", "Null", "NULL", "{{IMEI}}", "{{ANDDROID_ID}}", "{{OAID}}", "", "__IMEI__", "__OAID__");

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    PikaTemplate pikaTemplate;

    @Autowired
    ChannelConfigService channelConfigService;

    @Autowired
    FeedbackConfigService feedbackConfigService;

    @Override
    public void saveClickLog(ClickLog clickLog, String clickType) {
        Assert.notNull(clickLog, "clickLog不能为空");
        Assert.notNull(clickType, "clickType不能为空");

        List<String> keys = getClickLogKeys(clickLog, clickType);
        List<HashKeyFields> keyFieldsList = getAllClickLogIdRedisKeyFields(keys, clickLog.getChannel());

        //coid ncoid channel 为null时，存储数据无意义，不保存
        if (!CollectionUtils.isEmpty(keyFieldsList)) {
            UniqueClickLog uniqueClickLog = new UniqueClickLog(clickType, clickLog.getId());
            keyFieldsList.forEach(keyFields -> {
                doSaveClickLogId(keyFields, uniqueClickLog);
            });
            doSaveClickLog(clickType, clickLog);
        } else {
            log.warn("clicklog无法生成有效的hashKey，type:{},id:{}", clickType, clickLog.getId());
        }
    }

    private List<String> getClickLogKeys(ClickLog clickLog, String clickType) {
        List<String> keys = Lists.newArrayList(clickLog.getImeiMd5(), clickLog.getOaidMd5(), clickLog.getAndroidIdMd5());
        if (ClickType.BAIDU.getType().equals(clickType)) {
            keys.add(clickLog.getIpua());
        }
        return keys.stream().filter(k -> !excludeKeys.contains(k) && !k.startsWith("FAKE") && !k.startsWith("{{")).collect(Collectors.toList());
    }

    /**
     * 获取点击日志Id 所有的Redis相关的Key和HashField
     *
     * @param keys
     * @param channel
     * @return
     */
    private List<HashKeyFields> getAllClickLogIdRedisKeyFields(List<String> keys, String channel) {
        List<HashKeyFields> keyFieldsList = Lists.newArrayList();
        keys.forEach(key -> {
            HashKeyFields keyFields = getClickLogIdRedisKeyFields(key, channel);
            if (!CollectionUtils.isEmpty(keyFields.getHashFields())) {
                keyFieldsList.add(keyFields);
            }
        });
        return keyFieldsList;
    }

    /**
     * 获取点击日志Id的Redis相关的Key和HashField
     */
    private HashKeyFields getClickLogIdRedisKeyFields(String key, String channel) {
        String redisKey = RedisKeyManager.getClickLogIdKey(key);
        Set<String> hashField = Sets.newHashSet();
        String channelField = channel;
        //重构后 hashField中只保存一个channel
        if (channelField != null) {
            hashField.add(channelField);
        }
        return new HashKeyFields().setRedisKey(redisKey).setHashFields(hashField);
    }

    /**
     * 保存点击日志Id
     *
     * @param keyFields
     */
    private void doSaveClickLogId(HashKeyFields keyFields, UniqueClickLog uniqueClickLog) {
        if (!CollectionUtils.isEmpty(keyFields.getHashFields())) {
            boolean isUsePika = Lists.newArrayList(ClickType.GDT.getType(), ClickType.TOUTIAO.getType()).contains(uniqueClickLog.getClickType());
            RedisTemplate storeTemplate = isUsePika ? pikaTemplate : redisTemplate;
            String uniqueId = uniqueClickLog.ToUniqueId();
            Map<String, String> map = keyFields.getHashFields().stream().collect(Collectors.toMap(f -> f, f -> uniqueId));
            storeTemplate.opsForHash().putAll(keyFields.getRedisKey(), map);
            storeTemplate.expire(keyFields.getRedisKey(), 5, TimeUnit.DAYS);
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
        boolean isAllMatch = getIsAllMatch(clickLog.getChannel());
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, clickLog.getCoid(), clickLog.getNcoid(), clickLog.getChannel(),
                isAllMatch);
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
        RedisTemplate template = redisTemplate;
        Object obj = template.opsForValue().get(redisKey);
        return obj == null ? "" : obj.toString();
    }

    @Override
    public MatchClickLogResult matchClickLog(ActiveFeedbackMatch activeFeedbackMatch) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");

        //如果渠道被排除 则直接不匹配
        List<String> excludeChannels = feedbackConfigService.getExcludeChannels();
        if (excludeChannels.contains(activeFeedbackMatch.getChannel())) {
            return null;
        }

        MatchClickLogResult clickLogResult = null;
        List<ActiveMatchKeyField> matchKeyFields = getActiveMatchKeyFields(activeFeedbackMatch);
        //若渠道有设置需要匹配的关键字 则按顺序使用这些关键字
        List<String> matchFields = feedbackConfigService.getMatchFields(activeFeedbackMatch.getChannel());
        if (!CollectionUtils.isEmpty(matchFields)) {
            List<ActiveMatchKeyField> tempKeyFields = Lists.newArrayList();
            for (String matchField : matchFields) {
                tempKeyFields.addAll(matchKeyFields.stream().filter(mk -> matchField.equals(mk.getMatchField())).collect(Collectors.toList()));
            }
            matchKeyFields = tempKeyFields;
        }

        for (ActiveMatchKeyField keyField : matchKeyFields) {

            //获取点击key中存储的hash数据
            Map<String, String> clickLogIdMap = getUniqueClickLogIdMap(keyField.getMatchKey(), activeFeedbackMatch.getType());
            if (CollectionUtils.isEmpty(clickLogIdMap)) {
                continue;
            }

            //根据激活渠道获取点击数据
            String uniqueClickLogId = clickLogIdMap.getOrDefault(activeFeedbackMatch.getChannel(), null);

            //激活渠道中无法获取点击数据 查看其他点击渠道是否启用全网归因
            if (uniqueClickLogId == null) {
                for (Map.Entry<String, String> entry : clickLogIdMap.entrySet()) {
                    String channel = !entry.getKey().contains("_") ? entry.getKey() : null; //排除旧的coid_ncoid的key 其他key为channel
                    if (channel != null && !"0".equals(channel)) {
                        ThrowChannelConfig channelConfig = channelConfigService.getByChannel(channel);
                        //检查全网归因配置 & 产品相等
                        boolean isMatch = channelConfig != null && channelConfig.getChannelType().equals(0) && channelConfig.getCoid().equals(activeFeedbackMatch.getCoid())
                                && channelConfig.getNcoid().equals(activeFeedbackMatch.getNcoid());
                        if (isMatch) {
                            uniqueClickLogId = entry.getValue();
                            break;
                        }
                    }
                }
            }

            //无法匹配点击数据时 判断是否存在广点通渠道为0的全网归因
            if (uniqueClickLogId == null && clickLogIdMap.containsKey("0")) {
                String tempClickLogId = clickLogIdMap.get("0");
                UniqueClickLog tempClickLog = UniqueClickLog.FromUniqueId(tempClickLogId);
                String dsClick = DataSourcePicker.getDataSourceByClickType(tempClickLog.getClickType());
                //广点通无法获取渠道的数据启用全网归因
                boolean isMatch = DsConstants.GDT.equals(dsClick);
                if (isMatch) {
                    uniqueClickLogId = tempClickLogId;
                }
            }

            if (StringUtils.isNotBlank(uniqueClickLogId)) {
                UniqueClickLog uniqueClickLog = UniqueClickLog.FromUniqueId(uniqueClickLogId);
                clickLogResult = new MatchClickLogResult();
                clickLogResult.setClickType(uniqueClickLog.getClickType());
                clickLogResult.setClickLogId(uniqueClickLog.getClickLogId());
                clickLogResult.setMatchKey(keyField.getMatchKey());
                clickLogResult.setMatchField(keyField.getMatchField());
                return clickLogResult;
            }

        }
        return clickLogResult;
    }

    @Override
    public boolean checkMatchedBefore(ActiveFeedbackMatch activeFeedbackMatch) {
        List<ActiveMatchKeyField> matchKeyFields = getActiveMatchKeyFields(activeFeedbackMatch);
        //检查是否回传过
        List<String> keys = matchKeyFields.stream().map(kf -> kf.getMatchKey()).collect(Collectors.toList());
        boolean matchedBefore = checkKeysMatchedBefore(keys, activeFeedbackMatch);
        return matchedBefore;
    }

    /**
     * 获取回传所需要匹配的字段
     *
     * @param feedbackMatch
     * @return
     */
    private List<ActiveMatchKeyField> getActiveMatchKeyFields(ActiveFeedbackMatch feedbackMatch) {
        String iimei = feedbackMatch.getIimei();
        String imei = feedbackMatch.getImei();
        String oaid = feedbackMatch.getOaid();
        String androidid = feedbackMatch.getAndroidid();
        List<ActiveMatchKeyField> keys = Lists.newArrayList();

        //imei放在前面 优先匹配imei
        if (StringUtils.isNotBlank(iimei)) {
            for (String mei : iimei.split(",")) {
                keys.add(new ActiveMatchKeyField("imei", mei));
            }
        } else {
            keys.add(new ActiveMatchKeyField("imei", imei));
        }
        keys.add(new ActiveMatchKeyField("oaid", oaid));
        keys.add(new ActiveMatchKeyField("androidId", androidid));
        if (DsConstants.BAIDU.equals(feedbackMatch.getType())) {
            keys.add(new ActiveMatchKeyField("ipua", feedbackMatch.getIp() + "#" + feedbackMatch.getUa()));
        }

        keys = keys.stream().filter(k -> StringUtils.isNotBlank(k.getMatchKey()))
                .filter(k -> !excludeKeys.contains(k.getMatchKey()) && !k.getMatchKey().startsWith("FAKE") && !k.getMatchKey().startsWith("{{"))
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
    private boolean checkKeysMatchedBefore(List<String> keys, ActiveFeedbackMatch feedbackMatch) {
        RedisTemplate storeTemplate = redisTemplate;
        Integer eventType = feedbackMatch.getEventType();
        for (String key : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(key, feedbackMatch.getCoid(), feedbackMatch.getNcoid());
            if (eventType.equals(ACTIVE)) {
                if (storeTemplate.hasKey(redisKey)) {
                    return true;
                }
            } else {
                if ("1".equals(storeTemplate.opsForValue().get(redisKey))) return true;
            }
        }
        return false;
    }

    @Override
    public void saveMatchedFeedbackRecord(ActiveFeedbackMatch activeFeedbackMatch, ClickLog click) {
        Assert.notNull(activeFeedbackMatch, "activeFeedbackMatch不能为空");
        List<ActiveMatchKeyField> keys = getActiveMatchKeyFields(activeFeedbackMatch);
        doSaveMatchedFeedbackRecord(keys, click, activeFeedbackMatch);
    }

    private void doSaveMatchedFeedbackRecord(List<ActiveMatchKeyField> keys, ClickLog clickLog, ActiveFeedbackMatch feedbackMatch) {
        RedisTemplate storeTemplate = redisTemplate;
        for (ActiveMatchKeyField keyField : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(keyField.getMatchKey(), feedbackMatch.getCoid(), feedbackMatch.getNcoid());
            //ipua无法通过linkstatistics去重 永久保存在redis中
            String value = feedbackMatch.getEventType().equals(ACTIVE) ? String.format("%d_%s_%d", System.currentTimeMillis(), clickLog.getClickType(), clickLog.getId()) : "1";
            if (keyField.getMatchField().equals("ipua")) {
                storeTemplate.opsForValue().set(redisKey, value);
            } else {
                storeTemplate.opsForValue().set(redisKey, value, 3, TimeUnit.DAYS);
            }
            //clickChannel 持久化存储
            if (StringUtils.isNotBlank(clickLog.getChannel())) {
                pikaTemplate.opsForValue().set(redisKey, clickLog.getChannel());
            }
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

    @Override
    public MatchRetentionResult matchFeedbackRetentionClickLog(ActiveFeedbackMatch feedbackMatch) {
        List<ActiveMatchKeyField> matchKeyFields = getActiveMatchKeyFields(feedbackMatch);
        List<String> keys = matchKeyFields.stream().map(kf -> kf.getMatchKey()).collect(Collectors.toList());
        RedisTemplate storeTemplate = redisTemplate;
        for (String key : keys) {
            String redisKey = RedisKeyManager.getMatchedRedisKey(key, feedbackMatch.getCoid(), feedbackMatch.getNcoid());

            Object obj = storeTemplate.opsForValue().get(redisKey);
            String value = obj == null ? "" : String.valueOf(obj);
            if (StringUtils.isNotBlank(value) && !value.equals("1")) {
                String[] valueSplit = value.split("_");
                Date now = new Date();
                now = DateUtils.addDays(now, -1);
                SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
                boolean isYesterday = format.format(new Date(Long.valueOf(valueSplit[0]))).equals(format.format(now));
                if (isYesterday) {
                    return new MatchRetentionResult().setFeedbackDate(valueSplit[0]).setClickType(valueSplit[1]).setClickLogId(Long.valueOf(valueSplit[2]));
                }
            }
        }
        return null;
    }

    private Map<String, String> getUniqueClickLogIdMap(String matchKey, String activeType) {
        boolean isUsePika = Lists.newArrayList(DsConstants.GDT, DsConstants.TOUTIAO).contains(DataSourcePicker.getDataSourceByActiveType(activeType));
        RedisTemplate storeTemplate = isUsePika ? pikaTemplate : redisTemplate;
        String redisKey = RedisKeyManager.getClickLogIdKey(matchKey);
        Map<String, String> clickLogIdMap = storeTemplate.opsForHash().entries(redisKey);
        return clickLogIdMap;
    }

    @Override
    public void saveNewUserRetry(NewUserRetry newUserRetry) {
        if (newUserRetry == null || newUserRetry.getId() == 0) {
            throw new IllegalArgumentException("newUserRetry必须持久化有id");
        }

        ActiveFeedbackMatch feedbackMatch = new ActiveFeedbackMatch();
        BeanUtils.copyProperties(newUserRetry, feedbackMatch);
        List<String> keys = getActiveMatchKeys(feedbackMatch);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), null);
        String uniqueUserRetryId = RedisKeyManager.getUniqueUserRetryId(newUserRetry.getDataSource(), newUserRetry.getId());
        redisKeys.forEach(redisKey -> {
            doSaveNewUserRetryId(redisKey, uniqueUserRetryId);
        });
    }

    @Override
    public void deleteNewUserRetry(NewUserRetry newUserRetry) {
        Assert.notNull(newUserRetry, "newUserRetry cannot be null");

        ActiveFeedbackMatch feedbackMatch = new ActiveFeedbackMatch();
        BeanUtils.copyProperties(newUserRetry, feedbackMatch);
        List<String> keys = getActiveMatchKeys(feedbackMatch);
        List<String> redisKeys = getNewUserRetryIdRedisKeys(keys, newUserRetry.getCoid(), newUserRetry.getNcoid(),
                newUserRetry.getChannel(), null);
        redisKeys.forEach(redisKey -> {
            doDeleteNewUserRetryId(redisKey);
        });
    }

    /**
     * 获取渠道是否为全网归因
     *
     * @param channel
     * @return
     */
    private boolean getIsAllMatch(String channel) {
        ThrowChannelConfig channelConfig = channelConfigService.getByChannel(channel);
        return channelConfig != null && channelConfig.getChannelType() == 0;
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
        RedisTemplate template = redisTemplate;
        template.opsForValue().set(redisKey, id, 1, TimeUnit.DAYS);
    }

    private void doDeleteNewUserRetryId(String redisKey) {
        RedisTemplate template = redisTemplate;
        template.delete(redisKey);
    }

}
