package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ActiveLoggerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2019/12/14
 * Time: 20:13
 */
@DS("master_")
public interface ActiveLoggerHistoryDao extends JpaRepository<ActiveLoggerHistory, Long> {
}
