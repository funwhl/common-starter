package com.eighteen.common.feedback.constants;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Stream;

/**
 * Created by wangwei.
 * Date: 2020/4/11
 * Time: 13:44
 */
public interface Constants {
     List<String> excludeKeys = Lists.newArrayList(null, "null", "Unknown", "Null", "NULL", "{{IMEI}}", "{{ANDDROID_ID}}", "{{OAID}}", "", "__IMEI__", "__OAID__");

    interface RedisKeys {
        String FEED_BACK_CACHE = "feedback_";
        String FEED_BACK_CONFIG_WDS = "feedback_config_wds";
        String FEED_BACK_CONFIG_EXCLUDE_CHANNELS = "feedback_config_exclude_channels";
        String FEED_BACK_CONFIG_INCLUDE_CHANNELS = "feedback_config_include_channels";
        String FEED_BACK_CONFIG_INCLUDE_TYPE = "feedback_config_include_type";
    }

    interface Urls {
        String GDT_FEEDBACK_URL = "https://api.e.qq.com/v1.0/user_actions/add?access_token=<token>&timestamp=<timestamp>&nonce=<nonce>";
    }

    interface locks {
        String FEEDBACK_MATCH = "feedback#click";
    }

    interface queues {
        String FEEDBACK_LOG = "feedbacklog";
    }

    interface EventType {
        int ACTIVE = 1;
        // 次日留存
        int RETENTION_1 = 2;
        // 7日留存
        int RETENTION_7 = 3;
    }

    interface AdverType {
        int SHOW = 1;
        int CLICK = 2;
    }
    interface FeedbackType {
        int ACTIVE = 1;
        int SHOW = 2;
        int CLICK = 3;
    }

    interface FeedbackConfigType {
        int CHANNEL_EXCLUDE = 0;
        int CHANNEL_INCLUDE = 1;
        int CHANNEL_WD = 2;
        int FEEDBACK_TYPE_INCLUDE = 3;
    }

    enum FeedbackMatchFields {
        IMEI(1, "imei"),
        OAID(2, "oaid"),
        ANDROID_ID(3, "androidId"),
        IPUA(4, "ipua");

        Integer type;
        String desc;

        FeedbackMatchFields(int type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public static String getDesc(Integer type) {
            return Stream.of(FeedbackMatchFields.values()).filter(feedbackWd -> feedbackWd.type.equals(type)).findFirst().get().desc;
        }
    }

}
