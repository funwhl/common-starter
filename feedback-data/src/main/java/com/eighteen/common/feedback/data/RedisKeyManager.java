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

    public static String getClickLogIdKey(String key, Integer coid, Integer ncoid) {
        return String.format("%s%s_%d_%d", CLICK_LOG_KEY_COID_PREFIX, key, coid, ncoid);
    }

    public static String getClickLogIdKey(String key, String channel) {
        return String.format("%s%s_%s", CLICK_LOG_KEY_CHANNEL_PREFIX, key, channel);
    }

    public static String getClickLogDataKey(String id) {
        return String.format("%s%s", CLICK_LOG_ID_PREFIX, id);
    }

    public static String getNewUserRetryIdKey(String key, Integer coid, Integer ncoid) {
        return String.format("%s%s_%d_%d", NEW_USR_RETRY_KEY_COID_PREFIX, key, coid, ncoid);
    }

    public static String getNewUserRetryIdKey(String key, String channel) {
        return String.format("%s%s_%s", NEW_USER_RETRY_KEY_CHANNEL_PREFIX, key, channel);
    }
}
