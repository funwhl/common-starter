package com.eighteen.common.feedback.dao;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.domain.FeedbackConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * @author : wangwei
 * @date : 2020/4/25 11:26
 */
@org.apache.ibatis.annotations.Mapper
@DS("activestatistics")
public interface FeedbackConfigMapper extends Mapper<FeedbackConfig>, InsertListMapper<FeedbackConfig> {
    @Update("update activestatistics.t_feedback_config set value =  CONCAT(#{channel},',',value) where type = 1 and value not like '%#{channel}%' ")
    void updateGdtDebug(@Param("channel")String channel);
}
