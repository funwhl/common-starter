package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.FeedbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:06
 */
@DS("slave_1")
public interface FeedbackLogDao extends JpaRepository<FeedbackLog, Long> {
}
