package com.eighteen.common.feedback.dao;


import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

import java.util.List;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:39
 */

@org.apache.ibatis.annotations.Mapper
public interface ChannelConfigMapper {
    @Select("SELECT id,coid,ncoid,channel,agent,channel_type as channelType,plat_type as platType,remark,rate,ori_rate as oriRate,ad_filter as adFilter,feedback_type as feedbackType  FROM [DB2_33].[YYGJ].[dbo].[throw_channel_config] with(nolock) ")
    List<ThrowChannelConfig> throwChannelConfigList();

    @Select("SELECT id,coid,ncoid,channel,agent,channel_type as channelType,plat_type as platType,remark,rate,ori_rate as oriRate,ad_filter as adFilter,feedback_type as feedbackType  FROM [DB2_33].[YYGJ].[dbo].[throw_channel_config] with(nolock) where channel = #{channel} ")
    ThrowChannelConfig getOne(@Param("channel") String channel);

    @Insert("INSERT INTO [DB2_33].[YYGJ].[dbo].[throw_channel_config]  ( coid,ncoid,channel,agent,channel_type,plat_type,rate,ori_rate ) VALUES( #{config.coid},#{config.ncoid},#{config.channel},#{config.agent},#{config.channelType},#{config.platType},1,1)")
    int insert(@Param("config") ThrowChannelConfig config);
}
