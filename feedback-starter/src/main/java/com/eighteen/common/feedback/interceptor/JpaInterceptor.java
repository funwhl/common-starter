package com.eighteen.common.feedback.interceptor;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Created by wangwei.
 * Date: 2020/3/24
 * Time: 3:50
 */
public class JpaInterceptor implements StatementInspector {
    @Override
    public String inspect(String sql) {
        if (sql.contains("inner join t_click_log")) {
            if (sql.contains("activelogg0_.imei_md5=clicklog1_.imei_md5")) {
                sql = sql.replace("t_active_logger activelogg0_ inner join t_click_log clicklog1_", "t_active_logger activelogg0_ with(nolock,index(activeTime)) inner join t_click_log clicklog1_ with(nolock,index(imei_md5))");
            } else if (sql.contains("activelogg0_.android_id_md5=clicklog1_.android_id_md5")) {
                sql = sql.replace("t_active_logger activelogg0_ inner join t_click_log clicklog1_", "t_active_logger activelogg0_ with(nolock,index(activeTime)) inner join t_click_log clicklog1_ with(nolock,index(android_id_md5))");
            } else if (sql.contains("activelogg0_.oaid_md5=clicklog1_.oaid_md5")) {
                sql = sql.replace("t_active_logger activelogg0_ inner join t_click_log clicklog1_", "t_active_logger activelogg0_ with(nolock,index(activeTime)) inner join t_click_log clicklog1_ with(nolock,index(oaid_md5))");
            } else sql = sql.replace("t_active_logger activelogg0_ inner join t_click_log clicklog1_", "t_active_logger activelogg0_ with(nolock,index(activeTime)) inner join t_click_log clicklog1_ with(nolock)");

        }
        return sql;
    }
}
