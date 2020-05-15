package com.eighteen.common.feedback.dao;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.domain.FeedbackConfig;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * @author : wangwei
 * @date : 2020/4/25 11:26
 */
@org.apache.ibatis.annotations.Mapper
@DS("activestatistics")
public interface FeedbackConfigMapper extends Mapper<FeedbackConfig>, InsertListMapper<FeedbackConfig> {

}
