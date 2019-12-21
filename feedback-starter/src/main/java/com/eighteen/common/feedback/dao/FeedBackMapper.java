package com.eighteen.common.feedback.dao;


import com.eighteen.common.feedback.domain.DayImei;
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
            " select top ${num} a.*, click.* from ActiveLogger a with(nolock) " +
            " INNER JOIN ClickLog click with(nolock) " +
            " ON a.imeimd5 = click.imei " +
            " and a.activetime >= click.click_time and a.status = 0 " +
            "</script>"
    )
    List<Map<String, Object>> getPreFetchData(@Param("num") Integer num);

    @Update("<script>" +
            "update ActiveLogger set status = #{status} where imei in " +
            "<foreach collection='imeis' index='index' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    long updateFeedbackStatus(@Param("imeis") List<String> imeis, @Param("status") Integer status);

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
            " and ncoid = #{ncoid}" +
            "</if>" +
            "<if test='ncoid != null'>" +
            " and ncoid = #{ncoid} " +
            "</if>" +
            "</script>")
    long countFromStatistics(@Param("wd") String wd,@Param("value") String value, @Param("coid") Integer coid, @Param("ncoid") Integer ncoid);

    @Insert("<script>" +
            " insert  into FeedbackLog" +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            ${key}" +
            "        </foreach>" +
            "        values " +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            #{params.${key}}" +
            "        </foreach>" +
            "</script>")
    long insertFeedback(@Param("params") Map params);

    @Insert("<script>" +
            " insert  into ActiveLogger " +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            ${key}" +
            "        </foreach>" +
            "        values " +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            #{params.${key}}" +
            "        </foreach>" +
            "</script>")
    long insertActiveLogger(@Param("params") Map params);

    @Select("<script>" +
            "select imei,channel, versionname versionName, coid, ncoid, wifimac,ip, activetime activeTime, type, ua, androidId, oaid from ThirdActive.dbo.${tableName} " +
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

//    @Insert("<script>" +
//            "Insert into ActiveLogger  " +
//            "SELECT imei, substring(sys.fn_sqlvarbasetostr(HashBytes('MD5',imei)),3,32) as imeimd5, channel, versionname, coid, ncoid, wifimac " +
//            ", substring(sys.fn_sqlvarbasetostr(HashBytes('MD5',wifimac)),3,32) as wifimacmd5, ip, activetime, type, ua, androidId, 0  " +
//            "from( " +
//            "SELECT " +
//            "*, row_number () OVER ( " +
//            "partition BY imei " +
//            "ORDER BY " +
//            "activetime DESC " +
//            ") rowid " +
//            "FROM ThirdActive.dbo.ActiveLogger " +
//            ")  t  WHERE rowid = 1 " +
//            " and type = #{channel} and activetime >= #{date} " +
//            "<if test='list != null and list.size() > 0'>" +
//            " and  imei not in " +
//            "<foreach collection='list' index='index' item='item' open='(' separator=',' close=')'>" +
//            "#{item}" +
//            "    </foreach>" +
//            "</if>" +
//            "</script>")
//    int syncActiveThird(@Param("list") Set<String> list, @Param("date") Date date, @Param("channel") String channel);

    @Delete("DELETE FROM ClickLog where create_time < #{end}")
    long cleanClickLog(@Param("end") Date end);

    @Insert("Insert into ActiveLogger_History select * from ActiveLogger " +
            " where ActiveLogger.activetime < #{end} ")
    long transferActiveToHistory(@Param("end") Date end);

    @Delete("delete from ActiveLogger where activetime < #{end}")
    long deleteActiveThird(@Param("end") Date end);

    @Insert("insert into DayImei (" +
            "      imei, imeimd5, CreateTime, coid, ncoid " +
            "    )" +
            "    values (" +
            "      #{imei}, #{imeimd5}, #{createTime}, #{coid}, #{ncoid} " +
            "    )")
    long insertDayImei(@Param("imei") String imei, @Param("imeimd5") String imeimd5, @Param("createTime") Date createTime, @Param("coid") Integer coid, @Param("ncoid") Integer ncoid);

    @Select("SELECT imei,coid,ncoid,imeimd5,createTime FROM DayImei where CreateTime > #{date} ")
    @ResultType(DayImei.class)
    List<DayImei> getDayImeis(@Param("date") Date date);

    @Delete("DELETE FROM DayImei where CreateTime < #{date} ")
    long cleanDayImeis(@Param("date") Date date);

    @Delete("DELETE FROM DayLiucunImei where CreateTime < #{date} ")
    long cleanDayLCImeis(@Param("date") Date date);

    @Insert("insert into ActiveStatisticsDayReport select a.channel,'${date}' as 'date' ,count(*) as 'count' ${did} from ActiveLogger a inner join ClickLog b " +
            " on a.imeimd5 = b.imei and activetime > #{date} and activetime < dateadd(day,1,#{date}) and a.activetime > b.click_time " +
            "GROUP BY a.channel ${did}")
    long activeStaticesDay(@Param("date") String date, @Param("did") String did);

    @Select("<script>" +
            "     insert into ${tableName} " +
            "      <foreach collection='params[0].keys' item='key' open='(' close=')' separator=',' >  " +
            "         ${key} " +
            "      </foreach>  " +
            "      values " +
            "    <foreach collection ='params' item='item' separator =','>" +
            "      <foreach collection='item.keys'  item='key' open='(' close=')' separator=','>  " +
            "         #{item.${key}}  " +
            "      </foreach> " +
            "    </foreach >" +
            "</script>")
    long insert(@Param("tableName") String tableName, @Param("params") List<Map<String, Object>> list);

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

    List<DayHistory> getDayHistorys(Date date, String s);
}
