package com.eighteen.common.feedback.dao;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.feedback.entity.ActiveLogger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by wangwei.
 * Date: 2020/3/12
 * Time: 17:47
 */
@DS("linkStat_")
@Mapper
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
public interface LinkedStasticMapper {
    @Select("<script>" +
            "select top ${count}  imei,iimei,channel, coid, ncoid, wifi,ip, firstLinkTime activeTime, ua, androidId, oaid,mid,ROW_NUMBER() OVER(ORDER BY firstLinkTime asc) AS r from active_feedback_match  with(nolock) " +
            "where 1=1 " +
            " and firstLinkTime >= #{date} and DATEPART(ss, firstLinkTime) &gt; = #{min} and DATEPART(ss, firstLinkTime) &lt; = #{max}  " +
            "</script>"
    )
    @ResultType(ActiveLogger.class)
    List<ActiveLogger> getThirdActiveLogger(@Param("count") Integer count, @Param("date") Date date, @Param("min") Integer sc, String min, @Param("max") String max);

}
