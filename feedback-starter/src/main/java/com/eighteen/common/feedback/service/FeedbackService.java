package com.eighteen.common.feedback.service;

import com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType;

import java.util.List;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:42
 */

public interface FeedbackService {
    void feedback();

    void syncActive();

    void clean(JobType type);

    void clearCache(Integer offset);

    void stat(JobType type);

    /**
     * 次日留存
     * @param type
     */
    void secondStay(JobType type);
}
