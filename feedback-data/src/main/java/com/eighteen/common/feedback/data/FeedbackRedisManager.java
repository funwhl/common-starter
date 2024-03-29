package com.eighteen.common.feedback.data;

import com.eighteen.common.feedback.domain.*;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.NewUserRetry;

public interface FeedbackRedisManager {

    /**
     * 保存点击日志
     *
     * @param clickLog
     * @param channelType
     */
    void saveClickLog(ClickLog clickLog, String channelType);

    /**
     * 保存激活重试数据
     *
     * @param newUserRetry
     */
    void saveNewUserRetry(NewUserRetry newUserRetry);

    /**
     * 从redis中删除newuserretry相关数据
     *
     * @param newUserRetry
     */
    void deleteNewUserRetry(NewUserRetry newUserRetry);

    /**
     * 保存广告上报
     *
     * @param adverLog
     */
    void saveAdverLog(AdverLog adverLog);

    void deleteAdverLog(AdverLog adverLog);

    MatchAdverLogResult matchAdverLog(ActiveFeedbackMatch activeFeedbackMatch);



    /**
     * 匹配激活重试数据Id
     *
     * @param clickLog
     * @return
     */
    MatchNewUserRetryResult matchNewUserRetry(ClickLog clickLog,String clickType);

    /**
     * 根据广告日志匹配NewUserRetry记录
     * @param adverLog
     * @return
     */
    MatchNewUserRetryResult matchNewUserRetry(AdverLog adverLog);

    /**
     * 匹配点击Id
     *
     * @param activeFeedbackMatch
     * @return
     */
    MatchClickLogResult matchClickLog(ActiveFeedbackMatch activeFeedbackMatch);

    /**
     * 检查激活数据是否匹回传
     *
     * @param activeFeedbackMatch
     * @return
     */
    boolean checkMatchedBefore(ActiveFeedbackMatch activeFeedbackMatch);

    /**
     * 保存匹配回传记录
     *
     * @param activeFeedbackMatch
     * @param click               匹配到的点击记录中的渠道
     */
    void saveMatchedFeedbackRecord(ActiveFeedbackMatch activeFeedbackMatch, ClickLog click);

    /**
     * 从redis中获取点击日志
     *
     * @param
     * @return
     */
    ClickLog getClickLog(String clickType, Long clickLogId);

    MatchRetentionResult matchFeedbackRetentionClickLog(ActiveFeedbackMatch feedbackMatch);
}
