package com.eighteen.common.feedback.handler;

import com.eighteen.common.feedback.entity.ActiveLogger;

/**
 * Created by wangwei.
 * Date: 2020/3/10
 * Time: 17:20
 */
public interface NewUserHandler {
    String check(Integer sc, ActiveLogger activeLogger);
}
