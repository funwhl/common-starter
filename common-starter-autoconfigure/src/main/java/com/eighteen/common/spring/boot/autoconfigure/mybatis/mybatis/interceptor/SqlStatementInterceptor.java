package com.eighteen.common.spring.boot.autoconfigure.mybatis.mybatis.interceptor;//package com.eighteen.common.spring.boot.autoconfigure.mybatis.interceptor;
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.ibatis.cache.CacheKey;
//import org.apache.ibatis.executor.Executor;
//import org.apache.ibatis.mapping.BoundSql;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.ParameterMapping;
//import org.apache.ibatis.plugin.*;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.session.Configuration;
//import org.apache.ibatis.session.ResultHandler;
//import org.apache.ibatis.session.RowBounds;
//import org.apache.ibatis.type.TypeHandlerRegistry;
//import org.springframework.util.CollectionUtils;
//
//import java.text.DateFormat;
//import java.util.*;
//import java.util.regex.Matcher;
//
///**
// * Created by wangwei.
// * Date: 2019/10/3
// * Time: 22:53
// */
//@Intercepts(value = {
//        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
//                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
//                RowBounds.class, ResultHandler.class})})
//@Slf4j
//public class SqlStatementInterceptor implements Interceptor {
//
//    /*<br>    *如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号； 对参数是null和不是null的情况作了处理<br>　　*/
//    private static String getParameterValue(Object obj) {
//        String value = null;
//        if (obj instanceof String) {
//            value = "'" + obj.toString() + "'";
//        } else if (obj instanceof Date) {
//            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
//            value = "'" + formatter.format(new Date()) + "'";
//        } else {
//            if (obj != null) {
//                value = obj.toString();
//            } else {
//                value = "";
//            }
//
//        }
//        return value;
//    }
//
//    private static String showSql(Configuration configuration, BoundSql boundSql) {
//        Object parameterObject = boundSql.getParameterObject();
//        List<ParameterMapping> parameterMappings = new ArrayList<>(boundSql.getParameterMappings());
//        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");  // sql语句中多个空格都用一个空格代替
//        if (!CollectionUtils.isEmpty(parameterMappings) && parameterObject != null) {
//            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry(); // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换<br>　　　　　　　// 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
//            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
//                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
//
//            } else {
//                MetaObject metaObject = configuration.newMetaObject(parameterObject);// MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
//                for (ParameterMapping parameterMapping : parameterMappings) {
//                    String propertyName = parameterMapping.getProperty();
//                    if (metaObject.hasGetter(propertyName)) {
//                        Object obj = metaObject.getValue(propertyName);
//                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
//                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
//                        Object obj = boundSql.getAdditionalParameter(propertyName);  // 该分支是动态sql
//                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
//                    } else {
//                        sql = sql.replaceFirst("\\?", "缺失");
//                    }//打印出缺失，提醒该参数缺失并防止错位
//                }
//            }
//        }
//        return sql;
//    }
//
//    @Override
//    public Object intercept(Invocation invocation) throws Throwable {
//        Object returnValue;
//        long start = System.currentTimeMillis();
//        returnValue = invocation.proceed();
//        long end = System.currentTimeMillis();
//        long time = end - start;
//        try {
//            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
//            Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
//            log.info("execute {} : {} in {}ms", ms.getId(), showSql(ms.getConfiguration(),ms.getBoundSql(parameter)), time);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return returnValue;
//    }
//
//    @Override
//    public Object plugin(Object arg0) {
//        return Plugin.wrap(arg0, this);
//    }
//}