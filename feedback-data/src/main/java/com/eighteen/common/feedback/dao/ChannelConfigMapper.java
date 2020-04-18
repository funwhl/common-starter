package com.eighteen.common.feedback.dao;


import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:39
 */

@Mapper
public interface ChannelConfigMapper {
    @Select("SELECT id,coid,ncoid,channel,agent,channel_type as channelType,plat_type as platType,remark,rate,ori_rate as oriRate  FROM [DB2_33].[YYGJ].[dbo].[throw_channel_config] ")
    List<ThrowChannelConfig> throwChannelConfigList();
}
