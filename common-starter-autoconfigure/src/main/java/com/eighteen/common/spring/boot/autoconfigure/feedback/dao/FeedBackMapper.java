package com.eighteen.common.spring.boot.autoconfigure.feedback.dao;


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
    @Select("select top ${num} a.*, click.call_back as callback from KuaiShouFeedback.dbo.ActiveLogger_kuaishou a WITH ( nolock ) " +
            " INNER JOIN KuaiShouFeedback.dbo.KuaiShouClickLog click WITH ( nolock )" +
            " ON a.md5 = click.imei and a.activetime >= click.click_time " +
            " and not exists(select imei from DB_32.ActiveStatistics.dbo.LinkStatistics b with(nolock)" +
            " where a.imei=b.imei and a.coid= b.coid and a.ncoid = b.ncoid )"
    )
    List<Map<String, Object>> getPreFetchData(@Param("num") Integer num);

    @Select("<script>" +
            "select count(1) from ActiveLogger_History " +
            " where imei = #{imei} " +
            "        <if test='coid != null'>" +
                        " and coid = #{coid}" +
            "        </if>" +
            "        <if test='noid != null'>" +
                        " and noid = #{noid} " +
            "        </if>" +
            "</script>")
    Integer countFromStatistics(@Param("imei") String imei,@Param("coid")Integer coid,@Param("noid")Integer noid);

//    @Insert("insert into FeedbackLog (AID, CID, CSITE, CTYPE, MAC, MAC1, UA, UA1, IDFA, ANDROIDID, ANDROIDID1, IMEI, UUID, OPENUDID, UDID, OS, IP, TS, CONVERT_ID, CALLBACK_URL, CALLBACK_PRARAM," +
//            " EventType, CreateTime) values (#{aID,jdbcType=VARCHAR}, #{cID,jdbcType=VARCHAR}, #{cSITE,jdbcType=VARCHAR}, #{cTYPE,jdbcType=VARCHAR}, #{mAC,jdbcType=VARCHAR}," +
//            " #{mAC1,jdbcType=VARCHAR}, #{uA,jdbcType=VARCHAR}, #{uA1,jdbcType=VARCHAR}, #{iDFA,jdbcType=VARCHAR}, #{aNDROIDID,jdbcType=VARCHAR}, #{aNDROIDID1,jdbcType=VARCHAR}, " +
//            "#{iMEI,jdbcType=VARCHAR}, #{uUID,jdbcType=VARCHAR}, #{oPENUDID,jdbcType=VARCHAR}, #{uDID,jdbcType=VARCHAR}, #{oS,jdbcType=VARCHAR}, #{iP,jdbcType=VARCHAR}, #{tS,jdbcType=VARCHAR}, #{cONVERTID,jdbcType=VARCHAR}, #{cALLBACKURL,jdbcType=VARCHAR}, #{cALLBACKPRARAM,jdbcType=VARCHAR}, #{eventType,jdbcType=INTEGER}, getdate()) ")

    int insertFeedback(@Param("params") Map params);

//    @Insert(" insert into KuaiShouFeedback.dbo.KuaiShouClickLog (imei, aid, cid, did, mac, ip, android_id, ts, click_time,create_time, call_back) " +
//            "values (#{imei,jdbcType=VARCHAR}, #{aid,jdbcType=VARCHAR}, #{cid,jdbcType=VARCHAR}, #{did,jdbcType=VARCHAR}, " +
//            "#{mac,jdbcType=VARCHAR}, #{ip,jdbcType=VARCHAR}, #{androidId,jdbcType=VARCHAR}, #{ts,jdbcType=BIGINT}, #{clickDate,jdbcType=TIMESTAMP}, #{createTime,jdbcType=TIMESTAMP}, #{callBack,jdbcType=VARCHAR}) ")

    int insertClickLog(@Param("params") Map params);

    @Insert("<script>" +
            "Insert into KuaiShouFeedback.dbo.ActiveLogger_kuaishou  " +
            "select * from ThirdActive.dbo.ActiveLogger where channel = #{channel} and activetime >= #{date} " +
            "<if test='list != null and list.size() > 0'>" +
//            " and active.type = 'kuaishou' and  imei not in " +
            " and  imei not in " +
            "<foreach collection='list' index='index' item='item' open='(' separator=',' close=')'>" +
            "#{item}" +
            "    </foreach>" +
            "</if>" +
            "</script>")
    int syncActiveThird(@Param("list") Set<String> list, @Param("date") Date date, @Param("channel") Integer channel);

    @Delete("DELETE FROM KuaiShouFeedback.dbo.KuaiShouClickLog where create_time < #{end}")
    int cleanClickLog(@Param("end") Date end);

    @Insert("Insert into KuaiShouFeedback.dbo.ActiveLogger_History select * from KuaiShouFeedback.dbo.ActiveLogger_kuaishou " +
            " where KuaiShouFeedback.dbo.ActiveLogger_kuaishou.activetime < #{end} ")
    int transferActiveToHistory(@Param("end") Date end);

    //    @Delete("delete from KuaiShouFeedback.dbo.ActiveLogger_kuaishou where activetime < DATEADD(day, -#{expire}, #{date})")
    @Delete("delete from KuaiShouFeedback.dbo.ActiveLogger_kuaishou where activetime < #{end}")
    int deleteActiveThird(@Param("end") Date end);

    @Insert("insert into KuaiShouFeedback.dbo.DayImei (" +
            "      imei, imeimd5, CreateTime" +
            "    )" +
            "    values (" +
            "      #{imei,jdbcType=VARCHAR}, #{imeimd5,jdbcType=VARCHAR}, #{createTime,jdbcType=TIMESTAMP}" +
            "    )")
    int insertDayImei(@Param("imei") String imei, @Param("imeimd5") String imeimd5, @Param("createTime") Date createTime);

    //    @Select("SELECT imeimd5 FROM KuaiShouFeedback.dbo.DayImei where CreateTime > DATEADD(day, -1, #{date})")
    @Select("SELECT imeimd5 FROM KuaiShouFeedback.dbo.DayImei where CreateTime > #{date} ")
    Set<String> getDayImeis(@Param("date") Date date);

    //    @Delete("DELETE FROM KuaiShouFeedback.dbo.DayImei where CreateTime > DATEADD(day, -1, GETDATE())")
    @Delete("DELETE FROM KuaiShouFeedback.dbo.DayImei where CreateTime < #{date} ")
    int cleanDayImeis(@Param("date") Date date);
}
