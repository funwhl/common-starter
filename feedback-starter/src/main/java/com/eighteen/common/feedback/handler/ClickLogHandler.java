package com.eighteen.common.feedback.handler;

import com.eighteen.common.feedback.entity.ClickLog;

import java.util.Map;

/**
 * @author : wangwei
 * @date : 2019/12/24 20:24
 */
public interface ClickLogHandler {
    void handler(Map<String, Object> params,ClickLog clickLog);
}
