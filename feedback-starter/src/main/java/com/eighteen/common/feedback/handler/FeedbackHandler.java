package com.eighteen.common.feedback.handler;

import com.eighteen.common.feedback.entity.ClickLog;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 15:31
 */
public interface FeedbackHandler {
   Boolean handler(ClickLog url);
}
