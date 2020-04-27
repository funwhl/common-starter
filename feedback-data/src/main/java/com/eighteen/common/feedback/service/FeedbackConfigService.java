package com.eighteen.common.feedback.service;

import java.util.List;

/**
 * @author : wangwei
 * @date : 2020/4/25 13:31
 */
public interface FeedbackConfigService {
    /**
     * 获取渠道用于匹配回传的字段（imei、oaid...）
     * @param channel
     * @return
     */
    List<String> getMatchFields(String channel);

    /**
     * 获取排除回传的渠道
     * @return
     */
    List<String> getExcludeChannels();

    /**
     * 获取启动回传的渠道
     * @return
     */
    List<String> getIncludeChannels();

    /**
     * 获取启动回传的平台
     * @return
     */
    List<String> getIncludeTypes();

    void refreshCache(String key);
}
