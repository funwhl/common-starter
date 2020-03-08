package com.eighteen.common.feedback.dao;


import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.ActiveLogger;
import com.eighteen.common.feedback.entity.ClickLog;
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
            "select top 1000  imei,iimei,channel, versionname versionName, coid, ncoid, wifimac,ip, activetime activeTime, type, ua, androidId, oaid,mid ,ROW_NUMBER() OVER(ORDER BY activetime asc) AS r from ThirdActive.dbo.${tableName}  with(nolock) " +
            "where 1=1 " +
            "<if test='channel != null'> "+
            " and type in"+
            "<foreach collection='channel' index='index' item='item' open='(' separator=',' close=')'> #{item} </foreach>" +
            "</if>"+
//            " and activetime >= #{date} and channel % ${sc} = #{sd} " +
            " and activetime >= #{date} and DATEPART(ss, activetime) &gt;= #{min} and DATEPART(ss, activetime) &lt;= #{max} " +
            "</script>"
    )
    @ResultType(ActiveLogger.class)
    List<ActiveLogger> getThirdActiveLogger(@Param("channel") List<String> channel, @Param("tableName") String tableName, @Param("date") Date date, @Param("min") String min, @Param("max") String max);

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

    @Insert("insert into ActiveStatisticsDayReport select a.channel,'${date}' as 'date' ,count(*) as 'count' ${did} from ActiveLogger a inner join ClickLog b " +
            " on a.imeimd5 = b.imei and activetime > #{date} and activetime < dateadd(day,1,#{date}) and a.activetime > b.click_time " +
            "GROUP BY a.channel ${did}")
    long activeStaticesDay(@Param("date") String date, @Param("did") String did);

    @Select(" select CASE datediff(day,'2019-09-09',getdate()) % 2   WHEN 0 THEN 'ActiveLogger' else 'ActiveLogger_B' end ")
    String getTableName();

    @Select("select b.*, c.callback_url as callBack,c.aid,c.cid,c.mac,c.ts,c.android_id as androidId from ( " +
            " select top 2000 a.*  from toutiaofeedback.dbo.ThirdRetentionLog a  " +
            " where  a.activetime >=  CAST( DATEADD(DAY,-1,GETDATE()) as date) and a.type = #{channel} " +
            " and not exists ( select imei from t_day_history d where a.imei = d.value and d.wd = 'retention' ) " +
            " ) b INNER JOIN t_click_log c on b.imeimd5 = c.imei ")
    List<ThirdRetentionLog> getSecondStay(@Param("channel") String channel);

    @Select("select top 2000 id as param4,imei as imei_md5,oaid,android_id as android_id_md5" +
            ",channel,click_time,create_time,mac,ip,ts " +
            ",callback_url from ${table} where 1=1 and id > #{id} order by id asc ")
    List<ClickLog> selectlist(@Param("id") Long id, @Param("table") String table);

    //            "<foreach collection='values' index='index' item='item' open='(' separator=',' close=')'> #{item} </foreach>" +
    @Insert("<script>" +
            "insert into ${tableName}_history select * from ${tableName} where id in " +
            "<foreach collection='ids' index='index' item='item' open='(' separator=',' close=')'> #{item} </foreach>" +
            "</script>")
    void insertInTo(@Param("tableName") String tableName, @Param("ids") List<Long> ids);

    @Select("select wd,value,coid,ncoid,create_time from t_day_history with(nolock) where wd = #{wd} and create_time >= #{date} ")
    List<DayHistory> dayHistorys(@Param("wd") String wd, @Param("date") Date date);

    @Select("select count(1) from  t_day_history where wd = #{wd} and value = #{value} and coid = #{coid} and ncoid = #{ncoid}")
    Long count(@Param("wd") String wd, @Param("value") String value,@Param("coid") Integer coid, @Param("ncoid") Integer ncoid);
}
