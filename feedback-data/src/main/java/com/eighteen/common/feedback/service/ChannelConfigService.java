package com.eighteen.common.feedback.service;

import com.eighteen.common.feedback.domain.ThrowChannelConfig;

import java.util.List;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:42
 */

public interface ChannelConfigService {
    List<ThrowChannelConfig> getThrowChannelConfigs();
}
