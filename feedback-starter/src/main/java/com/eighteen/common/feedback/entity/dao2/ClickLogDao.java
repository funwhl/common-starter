package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:04
 */
@DS("master_")
public interface ClickLogDao extends JpaRepository<ClickLog, Long> {
}
