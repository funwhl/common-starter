package com.eighteen.common.feedback.service;

import java.util.List;

/**
 * @author : wangwei
 * @date : 2020/4/25 13:31
 */
public interface FeedbackConfigService {
    List<String> getWds(String channel);

    List<String> getExcludeChannels();

    List<String> getIncludeChannels();

    void refreshCache(String key);
}
