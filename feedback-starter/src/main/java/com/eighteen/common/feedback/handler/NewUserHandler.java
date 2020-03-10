package com.eighteen.common.feedback.handler;

import com.eighteen.common.feedback.entity.ActiveLogger;

import java.util.List;

/**
 * Created by wangwei.
 * Date: 2020/3/10
 * Time: 17:20
 */
public interface NewUserHandler {
    String check(List<String> channels, ActiveLogger activeLogger);
}
