package com.eighteen.common.feedback.constants;

import java.util.stream.Stream;

/**
 * Created by wangwei.
 * Date: 2020/4/11
 * Time: 13:44
 */
public interface Constants {
    interface keys {
        String FEED_BACK_CACHE = "feedback_";
        String FEED_BACK_CONFIG_WDS = "feedback_config_wds";
        String FEED_BACK_CONFIG_EXCLUDE_CHANNELS = "feedback_config_exclude_channels";
        String FEED_BACK_CONFIG_INCLUDE_CHANNELS = "feedback_config_include_channels";
    }

    interface urls {
        String GDT_FEEDBACK_URL = "https://api.e.qq.com/v1.0/user_actions/add?access_token=<token>&timestamp=<timestamp>&nonce=<nonce>";
    }

    interface locks {
        String FEEDBACK_MATCH = "feedback#click";
    }

    interface queues {
        String FEEDBACK_LOG = "feedbacklog";
    }

    interface FEEDBACK_CONFIG_TYPE {
        int CHANNEL_EXCLUDE = 0;
        int CHANNEL_INCLUDE = 1;
        int CHANNEL_WD = 2;
    }

    enum FeedbackWd {
        IMEI(1, "imei"),
        OAID(2, "oaid"),
        ANDROID_ID(3, "androidId"),
        IPUA(4, "ipua");

        Integer type;
        String desc;

        FeedbackWd(int type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public static String getDesc(Integer type) {
            return Stream.of(FeedbackWd.values()).filter(feedbackWd -> feedbackWd.type.equals(type)).findFirst().get().desc;
        }
    }

}
