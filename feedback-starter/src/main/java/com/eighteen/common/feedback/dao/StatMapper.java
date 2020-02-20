package com.eighteen.common.feedback.dao;

import com.eighteen.common.feedback.domain.HourStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author : wangwei
 * @date : 2019/10/28 10:03
 */
@Mapper
public interface StatMapper {
    @Select(" select * from ( " +
            " select CAST( active_time as date) as date ,convert(numeric(10),DATENAME(HOUR, active_time))  as hour,COUNT(distinct imei) as count,'激活' as type " +
            " from t_active_Logger where active_time >= #{date} group by CAST( active_time as date) " +
            " ,convert(numeric(10),DATENAME(HOUR, active_time)) " +

            " UNION all " +

            " select CAST( create_time as date) as date ,convert(numeric(10),DATENAME(HOUR, create_time))  as hour,COUNT(imei) as count,'回传' as type from t_feedback_Log " +
            " where create_time >= #{date} " +
            " GROUP BY CAST( create_time as date),DATENAME(HOUR, create_time) " +

            " UNION all " +

            " select CAST( click_time as date) as date ,convert(numeric(10),DATENAME(HOUR, click_time))  as hour,COUNT(imei) as count, '点击' as type " +
            " from t_click_log where click_time >= #{date} group by CAST( click_time as date) " +
            " ,convert(numeric(10),DATENAME(HOUR, click_time)) " +

            " ) t1 where date = #{date} and hour = #{hour} ORDER BY  date describe,hour describe,type describe")
    List<HourStat> statByHour(@Param("date") String date, @Param("hour") Integer hour);

    @Select(" select count(1) from t_feedback where event_type = 1 and create_time >= #{date} ")
    Long statMiniute(@Param("date")Date date);
}
