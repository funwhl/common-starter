package com.eighteen.common.feedback.dao;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ActiveLogger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * Created by wangwei.
 * Date: 2020/3/5
 * Time: 19:33
 */
@DS("webLog_")
@Mapper
public interface WebLogMapper {
    @Select("<script>" +
            "select top 10000  imei,iimei,channel, versionname versionName, coid, ncoid, wifimac,ip, activetime activeTime, ua, androidId, oaid,mid ,ROW_NUMBER() OVER(ORDER BY activetime asc) AS r from ${tableName}  with(nolock) " +
            "where 1=1 " +
//            "<if test='channel != null'> "+
//            " and type in"+
//            "<foreach collection='channel' index='index' item='item' open='(' separator=',' close=')'> #{item} </foreach>" +
//            "</if>"+
//            " and activetime >= #{date} and channel % ${sc} = #{sd} " +
            " and activetime >= #{date} and DATEPART(ss, activetime) &gt; = #{min} and DATEPART(ss, activetime) &lt; = #{max}  " +
            "</script>"
    )
    @ResultType(ActiveLogger.class)
    List<ActiveLogger> getThirdActiveLogger(@Param("tableName") String tableName, @Param("date") Date date, @Param("min") Integer sc, String min, @Param("max") String max);

    @Select(" select CASE datediff(day,'2019-09-09',getdate()) % 2   WHEN 0 THEN 'ActiveLogger_B' else 'ActiveLogger' end ")
    String getTableName();
}
