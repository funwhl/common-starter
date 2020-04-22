package com.eighteen.common.feedback.data;

import lombok.experimental.UtilityClass;

/**
 * @author lcomplete
 */
@UtilityClass
public class RedisKeyManager {

    private static final String CLICK_LOG_KEY_CHANNEL_PREFIX = "ch_";

    private static final String CLICK_LOG_KEY_COID_PREFIX = "co_";

    private static final String CLICK_LOG_ID_PREFIX = "ci_";

    private static final String NEW_USER_RETRY_KEY_CHANNEL_PREFIX = "nh_";

    private static final String NEW_USR_RETRY_KEY_COID_PREFIX = "no_";

    private static final String MATCHED_KEY_PREFIX = "ma_";

    public static String getClickLogIdKey(String key, Integer coid, Integer ncoid) {
        return String.format("%s%d_%d_%s", CLICK_LOG_KEY_COID_PREFIX, coid, ncoid, key);
    }

    public static String getClickLogIdKey(String key, String channel) {
        return String.format("%s%s_%s", CLICK_LOG_KEY_CHANNEL_PREFIX, channel, key);
    }

    public static String getUniqueClickLogId(String clickType, Long clickLogId) {
        String uniqueClickLogId = String.format("%s_%d", clickType, clickLogId);
        return uniqueClickLogId;
    }

    public static String getUniqueUserRetryId(String dataSource, Long newUserRetryId) {
        String uniqueUserRetryId = String.format("%s_%d", dataSource, newUserRetryId);
        return uniqueUserRetryId;
    }

    public static String getClickLogDataKey(String clickType, Long clickLogId) {
        String uniqueClickLogId = getUniqueClickLogId(clickType, clickLogId);
        return String.format("%s%s", CLICK_LOG_ID_PREFIX, uniqueClickLogId);
    }

    public static String getNewUserRetryIdKey(String key, Integer coid, Integer ncoid) {
        return String.format("%s%d_%d_%s", NEW_USR_RETRY_KEY_COID_PREFIX, coid, ncoid, key);
    }

    public static String getNewUserRetryIdKey(String key, String channel) {
        return String.format("%s%s_%s", NEW_USER_RETRY_KEY_CHANNEL_PREFIX, channel, key);
    }

    public static String getMatchedRedisKey(String key) {
        return String.format("%s%s", MATCHED_KEY_PREFIX, key);
    }
}
