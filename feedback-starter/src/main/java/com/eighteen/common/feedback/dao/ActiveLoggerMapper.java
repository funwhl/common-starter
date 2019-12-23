package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.entity.ActiveLogger;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * Created by wangwei.
 * Date: 2019/12/21
 * Time: 21:23
 */
@org.apache.ibatis.annotations.Mapper
public interface ActiveLoggerMapper extends Mapper<ActiveLogger>, InsertListMapper<ActiveLogger> {
}
