package com.eighteen.common.feedback.entity.dao2;

import com.eighteen.common.feedback.entity.ActiveLogger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:04
 */

public interface ActiveLoggerDao extends JpaRepository<ActiveLogger, Long> {
    @Query(value = "select t from ActiveLogger t")
    List<ActiveLogger> findTest2();
}
