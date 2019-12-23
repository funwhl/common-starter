package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.entity.ClickLog;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:04
 */
@org.apache.ibatis.annotations.Mapper
public interface ClickLogMapper extends Mapper<ClickLog>, InsertListMapper<ClickLog> {
}
