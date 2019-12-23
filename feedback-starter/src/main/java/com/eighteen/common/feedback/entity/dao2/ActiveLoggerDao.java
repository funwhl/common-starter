package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ActiveLogger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:04
 */
@DS("master_")
public interface ActiveLoggerDao extends JpaRepository<ActiveLogger, Long> {

}
