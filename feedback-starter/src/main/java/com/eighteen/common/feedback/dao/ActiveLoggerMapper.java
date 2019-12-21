package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.entity.ActiveLogger;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * Created by wangwei.
 * Date: 2019/12/21
 * Time: 21:23
 */
public interface ActiveLoggerMapper extends Mapper<ActiveLogger>, InsertListMapper<ActiveLogger> {
}
