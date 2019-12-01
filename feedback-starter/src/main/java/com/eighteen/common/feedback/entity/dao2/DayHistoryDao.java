package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.DayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:05
 */
@DS("slave_1")
public interface DayHistoryDao extends JpaRepository<DayHistory, Long> {
}
