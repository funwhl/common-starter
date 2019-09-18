package com.eighteen.common.spring.boot.autoconfigure.feedback.dao;


import com.eighteen.common.spring.boot.autoconfigure.feedback.model.ThirdRetentionLog;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:39
 */

@Mapper
public interface FeedBackMapper {
    @Select("select top ${num} a.*, click.* from ActiveLogger a " +
            " INNER JOIN KuaiShouClickLog click" +
            " ON a.imeimd5 = click.imei and a.activetime >= click.click_time and a.status = 0 "
    )
    List<Map<String, Object>> getPreFetchData(@Param("num") Integer num);

    @Update("<script>" +
            "update ActiveLogger set status = #{status} where imei in " +
            "<foreach collection='imeis' index='index' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    int updateFeedbackStatus(@Param("imeis") List<String> imeis, @Param("status") Integer status);

    @Select("<script>" +
            "select count(1) from DB_32.ActiveStatistics.dbo.LinkStatistics " +
            " where imei = #{imei} " +
            "        <if test='coid != null'>" +
            " and ncoid = #{ncoid}" +
            "        </if>" +
            "        <if test='ncoid != null'>" +
            " and ncoid = #{ncoid} " +
            "        </if>" +
            "</script>")
    Integer countFromStatistics(@Param("imei") String imei, @Param("coid") Integer coid, @Param("ncoid") Integer ncoid);

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
    int insertFeedback(@Param("params") Map params);

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
    int insertActiveLogger(@Param("params") Map params);

    @Select("<script>" +
            "select imei,channel, versionname, coid, ncoid, wifimac,ip, activetime, type, ua, androidId from ThirdActive.dbo.${tableName} " +
            "where type = #{channel} and activetime >= ( select max(activetime) FROM ActiveLogger ) " +
            "</script>"
    )
    List<Map<String, Object>> getThirdActiveLogger(@Param("channel") String channel, @Param("tableName") String tableName);

    @Insert("<script>" +
            "insert  into KuaiShouClickLog" +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            ${key}" +
            "        </foreach>" +
            "        values" +
            "        <foreach collection='params.keys' item='key' open='(' close=')' separator=','>" +
            "            #{params.${key}}" +
            "        </foreach>" +
            "</script>")
    int insertClickLog(@Param("params") Map params);

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

    @Delete("DELETE FROM KuaiShouClickLog where create_time < #{end}")
    int cleanClickLog(@Param("end") Date end);

    @Insert("Insert into ActiveLogger_History select * from ActiveLogger " +
            " where ActiveLogger.activetime < #{end} ")
    int transferActiveToHistory(@Param("end") Date end);

    @Delete("delete from ActiveLogger where activetime < #{end}")
    int deleteActiveThird(@Param("end") Date end);

    @Insert("insert into DayImei (" +
            "      imei, imeimd5, CreateTime" +
            "    )" +
            "    values (" +
            "      #{imei,jdbcType=VARCHAR}, #{imeimd5,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP}" +
            "    )")
    int insertDayImei(@Param("imei") String imei, @Param("imeimd5") String imeimd5, @Param("createTime") Date createTime);

    @Select("SELECT imei FROM DayImei where CreateTime > #{date} ")
    Set<String> getDayImeis(@Param("date") Date date);

    @Delete("DELETE FROM DayImei where CreateTime < #{date} ")
    int cleanDayImeis(@Param("date") Date date);

    @Insert("insert into ActiveStatisticsDayReport select a.channel,${date} as 'date' ,count(*) as 'count' ${did} from ActiveLogger a inner join KuaiShouClickLog b " +
            " on a.imeimd5 = b.imei and activetime > #{date} and activetime < dateadd(day,1,#{date}) and a.activetime > b.click_time " +
            "GROUP BY a.channel ${did}")
    int activeStaticesDay(@Param("date") String date, @Param("did") String did);

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
    int insert(@Param("tableName") String tableName, @Param("params") List<Map<String, Object>> list);

    @Select(" select CASE datediff(day,'2019-09-09',getdate()) % 2   WHEN 0 THEN 'ActiveLogger' else 'ActiveLogger_B' end ")
    String getTableName();

    @Select("select b.*, c.call_back as callBack  from ( " +
            " select top 2000 a.*  from toutiaofeedback.dbo.ThirdRetentionLog a  " +
            " where  a.activetime >=  CAST( DATEADD(DAY,-1,GETDATE()) as date) and a.type ='kuaishouChannel'  " +
            " and not exists ( select imei from dayliucunimei ) " +
            " ) b INNER JOIN KuaiShouClickLog c on b.imeimd5 = c.imei " )
    List<ThirdRetentionLog> getSecondStay();

}
