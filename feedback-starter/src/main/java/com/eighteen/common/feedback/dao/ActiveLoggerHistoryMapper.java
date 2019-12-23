package com.eighteen.common.feedback.dao;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ActiveLoggerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * Created by wangwei.
 * Date: 2019/12/14
 * Time: 20:13
 */
@org.apache.ibatis.annotations.Mapper
public interface ActiveLoggerHistoryMapper extends Mapper<ActiveLoggerHistory>, InsertListMapper<ActiveLoggerHistory> {
}
