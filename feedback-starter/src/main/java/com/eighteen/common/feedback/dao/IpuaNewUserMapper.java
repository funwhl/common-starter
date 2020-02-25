package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.entity.IpuaNewUser;
import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * Created by wangwei.
 * Date: 2020/2/25
 * Time: 12:24
 */
@org.apache.ibatis.annotations.Mapper
public interface IpuaNewUserMapper extends Mapper<IpuaNewUser>, InsertListMapper<IpuaNewUser> {
}
