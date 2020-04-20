package com.eighteen.common.feedback.data;

import com.eighteen.common.feedback.domain.MatchClickLogResult;
import com.eighteen.common.feedback.domain.MatchNewUserRetryResult;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.NewUserRetry;

import java.util.List;

public interface FeedbackRedisManager {

    /**
     * 保存点击日志
     * @param clickLog
     * @param channelType
     */
    void saveClickLog(ClickLog clickLog, String channelType);

    /**
     * 保存激活重试数据
     * @param newUserRetry
     */
    void saveNewUserRetry(NewUserRetry newUserRetry);

    /**
     * 匹配激活重试数据Id
     * @param clickLog
     * @param channelType
     * @return
     */
    MatchNewUserRetryResult matchUniqueNewUserRetryId(ClickLog clickLog, String channelType);

    /**
     * 匹配点击Id
     * @param activeFeedbackMatch
     * @return
     */
    MatchClickLogResult matchUniqueClickLogId(ActiveFeedbackMatch activeFeedbackMatch);

    /**
     * 保存匹配回传记录
     * @param activeFeedbackMatch
     */
    void saveMatchedFeedbackRecord(ActiveFeedbackMatch activeFeedbackMatch);

    /**
     * 从redis中获取点击日志
     * @param
     * @return
     */
    ClickLog getClickLog(String channelType,Long clickLogId);

}
