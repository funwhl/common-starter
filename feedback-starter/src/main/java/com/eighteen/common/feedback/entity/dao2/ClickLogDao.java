package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:04
 */
@DS("slave_1")
public interface ClickLogDao extends JpaRepository<ClickLog, Long> {
}
