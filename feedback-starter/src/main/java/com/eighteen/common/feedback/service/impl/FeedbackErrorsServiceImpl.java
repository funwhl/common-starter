package com.eighteen.common.feedback.service.impl;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.FeedbackErrors;
import com.eighteen.common.feedback.entity.dao2.FeedbackErrorsDao;
import com.eighteen.common.feedback.service.FeedbackErrorsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by wangwei.
 * Date: 2020/3/12
 * Time: 19:19
 */
@Service
@DS("linkStat_")
public class FeedbackErrorsServiceImpl implements FeedbackErrorsService {
    @Autowired
    FeedbackErrorsDao feedbackErrorsDao;
    @Override
    public int insert(FeedbackErrors feedbackErrors) {
         feedbackErrorsDao.save(feedbackErrors);
        return 1;
    }
}
