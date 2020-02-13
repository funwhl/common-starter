package com.eighteen.common.feedback.dao;


import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.ActiveLogger;
import com.eighteen.common.feedback.entity.DayHistory;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:39
 */

@Mapper
public interface FeedBackMapper {
    @Select("<script>" +
            "select count(1) from LinkStatistics.dbo.LinkStatistics " +
            " where 1=1 " +
            "<if test='wd == \"imei\"'>" +
            " and imei = #{value}" +
            "</if>" +
            "<if test='wd == \"oaid\"'>" +
            " and oaid = #{value}" +
            "</if>" +
            "<if test='wd == \"androidId\"'>" +
            " and androidId = #{value}" +
            "</if>" +
            "<if test='wd == \"wifimac\"'>" +
            " and wifimac = #{value}" +
            "</if>" +
            "<if test='coid != null'>" +
            " and coid = #{coid}" +
            "</if>" +
            "<if test='ncoid != null'>" +
            " and ncoid = #{ncoid} " +
            "</if>" +
            "</script>")
    long countFromStatistics(@Param("wd") String wd, @Param("value") String value, @Param("coid") Integer coid, @Param("ncoid") Integer ncoid);

    @Select("<script>" +
            "select ${wd} as value,coid,ncoid from LinkStatistics.dbo.LinkStatistics " +
            " where 1=1 " +
            " and ${wd} in " +
            "<foreach collection='values' index='index' item='item' open='(' separator=',' close=')'> #{item} </foreach>" +
            "<if test='coid != null'> and coid = #{coid} </if>" +
            "<if test='ncoid != null'> and ncoid = #{ncoid} </if>" +
            "</script>")
    List<DayHistory> listFromStatistics(@Param("wd") String wd, @Param("values") List<String> values, @Param("coid") Integer coid, @Param("ncoid") Integer ncoid);


    @Select("<script>" +
            "select imei,channel, versionname versionName, coid, ncoid, wifimac,ip, activetime activeTime, type, ua, androidId, oaid,mid from ThirdActive.dbo.${tableName} " +
            "where type = #{channel} and activetime >= ( select CASE  when max(active_time) is null then cast(getdate() as date) else max(active_time) end FROM t_active_logger ) " +
            "</script>"
    )
    @ResultType(ActiveLogger.class)
    List<ActiveLogger> getThirdActiveLogger(@Param("channel") String channel, @Param("tableName") String tableName);

    @Insert("<script>" +
            "insert  into ClickLog" +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            ${key}" +
            "        </foreach>" +
            "        values" +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            #{params.${key}}" +
            "        </foreach>" +
            "</script>")
    long insertClickLog(@Param("params") Map params);


    @Delete("DELETE FROM DayLiucunImei where CreateTime < #{date} ")
    long cleanDayLCImeis(@Param("date") Date date);

    @Insert("insert into ActiveStatisticsDayReport select a.channel,'${date}' as 'date' ,count(*) as 'count' ${did} from ActiveLogger a inner join ClickLog b " +
            " on a.imeimd5 = b.imei and activetime > #{date} and activetime < dateadd(day,1,#{date}) and a.activetime > b.click_time " +
            "GROUP BY a.channel ${did}")
    long activeStaticesDay(@Param("date") String date, @Param("did") String did);

    @Select(" select CASE datediff(day,'2019-09-09',getdate()) % 2   WHEN 0 THEN 'ActiveLogger' else 'ActiveLogger_B' end ")
    String getTableName();

    @Select("select b.*, c.callback_url as callBack,c.aid,c.cid,c.mac,c.ts,c.android_id as androidId from ( " +
            " select top 2000 a.*  from toutiaofeedback.dbo.ThirdRetentionLog a  " +
            " where  a.activetime >=  CAST( DATEADD(DAY,-1,GETDATE()) as date) and a.type ='kuaishouChannel'  " +
            " and not exists ( select imei from dayliucunimei d where a.imei = d.imei ) " +
            " ) b INNER JOIN t_click_log c on b.imeimd5 = c.imei ")
    List<ThirdRetentionLog> getSecondStay();


    @Insert("insert into DayLiucunImei(imei ,imeimd5, createTime) VALUES (#{imei}, #{imeimd5} , getdate())")
    long insertDayLiucunImei(@Param("imei") String imei, @Param("imeimd5") String imeimd5);

}
