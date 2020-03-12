package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.FeedbackErrors;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2020/3/12
 * Time: 17:23
 */
@DS("master_")
public interface FeedbackErrorsDao extends JpaRepository<FeedbackErrors, Long> {
}
