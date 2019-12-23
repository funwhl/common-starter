package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.entity.FeedbackLog;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 18:06
 */
@org.apache.ibatis.annotations.Mapper
public interface FeedbackLogMapper extends Mapper<FeedbackLog>, InsertListMapper<FeedbackLog> {
}
