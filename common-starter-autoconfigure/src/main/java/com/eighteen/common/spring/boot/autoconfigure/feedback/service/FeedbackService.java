package com.eighteen.common.spring.boot.autoconfigure.feedback.service;

import com.eighteen.common.spring.boot.autoconfigure.feedback.service.impl.FeedbackServiceImpl.JobType;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:42
 */

public interface FeedbackService {
    void feedback();

    void syncActive();

    void clean(JobType type);

    void stat(JobType type);
}
