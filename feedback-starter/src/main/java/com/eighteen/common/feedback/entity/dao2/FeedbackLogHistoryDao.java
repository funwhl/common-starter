package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.feedback.entity.FeedbackLogHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:06
 */

public interface FeedbackLogHistoryDao extends JpaRepository<FeedbackLogHistory, Long> {
}
