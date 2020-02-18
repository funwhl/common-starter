package com.eighteen.common.feedback.handler;

import com.eighteen.common.feedback.entity.ActiveLogger;

/**
 * Created by wangwei.
 * Date: 2020/2/18
 * Time: 18:32
 */
public interface ActiveHandler {
    void handler(ActiveLogger activeLogger);
}
