package com.eighteen.common.spring.boot.autoconfigure.mybatis.desp;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wangwei.
 * Date: 2019/9/21
 * Time: 22:39
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class PageInterceptor implements Interceptor {
    private String databaseType;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler  statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = MetaObject.forObject(statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        String id = mappedStatement.getId();
        if (id.endsWith("Page")){
            BoundSql boundSql = statementHandler.getBoundSql();
            Map<String, Object> map = (Map<String, Object>) boundSql.getParameterObject();
            //需要在具体查询方法Page参数前添加@Param("page")
//            Page<Area> page = (Page<Area>) map.get("page");
            String sql = boundSql.getSql();
            String countSql = String.format("select count(*) from (%s) a", sql);
            Connection connection = (Connection) invocation.getArgs()[0];
            PreparedStatement preparedStatement = connection.prepareStatement(countSql);
            ParameterHandler parameterHandler = (ParameterHandler) metaObject.getValue("delegate.parameterHandler");
            parameterHandler.setParameters(preparedStatement);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()){
//                page.setTotalPage(rs.getInt(1));
            }
//            String pageSql = sql + " limit " + (page.getIndexPage()-1)*page.getPageSize() + ", " + page.getPageSize();
//            metaObject.setValue("delegate.boundSql.sql", pageSql);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o,this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
