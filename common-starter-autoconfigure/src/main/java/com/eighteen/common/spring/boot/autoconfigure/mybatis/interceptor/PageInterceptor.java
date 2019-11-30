package com.eighteen.common.spring.boot.autoconfigure.mybatis.interceptor;

/**
 * Created by wangwei.
 * Date: 2019/9/22
 * Time: 10:40
 */

import com.eighteen.common.utils.Page;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 分页拦截器，用于拦截需要进行分页查询的操作，然后对其进行分页处理。
 * 利用拦截器实现Mybatis分页的原理：
 * 要利用JDBC对数据库进行操作就必须要有一个对应的Statement对象，Mybatis在执行Sql语句前就会产生一个包含Sql语句的Statement对象，而且对应的Sql语句
 * 是在Statement之前产生的，所以我们就可以在它生成Statement之前对用来生成Statement的Sql语句下手。在Mybatis中Statement语句是通过RoutingStatementHandler对象的
 * prepare方法生成的。所以利用拦截器实现Mybatis分页的一个思路就是拦截StatementHandler接口的prepare方法，然后在拦截器方法中把Sql语句改成对应的分页查询Sql语句，之后再调用
 * StatementHandler对象的prepare方法，即调用invocation.proceed()。
 * 对于分页而言，在拦截器里面我们还需要做的一个操作就是统计满足当前条件的记录一共有多少，这是通过获取到了原始的Sql语句后，把它改为对应的统计语句再利用Mybatis封装好的参数和设
 * 置参数的功能把Sql语句中的参数进行替换，之后再执行查询记录数的Sql语句进行总记录数的统计。
 */

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class PageInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //对于StatementHandler其实只有两个实现类，一个是RoutingStatementHandler，另一个是抽象类BaseStatementHandler，
        //BaseStatementHandler有三个子类，分别是SimpleStatementHandler，PreparedStatementHandler和CallableStatementHandler，
        //SimpleStatementHandler是用于处理Statement的，PreparedStatementHandler是处理PreparedStatement的，而CallableStatementHandler是
        //处理CallableStatement的。Mybatis在进行Sql语句处理的时候都是建立的RoutingStatementHandler，而在RoutingStatementHandler里面拥有一个
        //StatementHandler类型的delegate属性，RoutingStatementHandler会依据Statement的不同建立对应的BaseStatementHandler，即SimpleStatementHandler、
        //PreparedStatementHandler或CallableStatementHandler，在RoutingStatementHandler里面所有StatementHandler接口方法的实现都是调用的delegate对应的方法。
        //我们在PageInterceptor类上已经用@Signature标记了该Interceptor只拦截StatementHandler接口的prepare方法，又因为Mybatis只有在建立RoutingStatementHandler的时候
        //是通过Interceptor的plugin方法进行包裹的，所以我们这里拦截到的目标对象肯定是RoutingStatementHandler对象。
//        RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
//        //通过反射获取到当前RoutingStatementHandler对象的delegate属性
//        StatementHandler delegate = (StatementHandler) ReflectUtil.getFieldValue(handler, "delegate");
//        //获取到当前StatementHandler的 boundSql，这里不管是调用handler.getBoundSql()还是直接调用delegate.getBoundSql()结果是一样的，因为之前已经说过了
//        //RoutingStatementHandler实现的所有StatementHandler接口方法里面都是调用的delegate对应的方法。
//        BoundSql boundSql = delegate.getBoundSql();
//        Object obj = boundSql.getParameterObject();

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = MetaObject.forObject(statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        String id = mappedStatement.getId();
        BoundSql boundSql = statementHandler.getBoundSql();
        Object obj = boundSql.getParameterObject();

        Page<?> page = null;
        Integer pageSize = null;
        Integer pageNo = null;
        if (obj instanceof Map) {
            Map<String, Object> objectMap = (Map<String, Object>) obj;
//            pageNo = objectMap.containsKey("pageNo")?1: (Integer) objectMap.get("pageNo");
//            pageSize = objectMap.containsKey("pageSize")?10: (Integer) objectMap.get("pageSize");
            for(Object o : objectMap.values()){
                if (o instanceof Page<?>) page = (Page<?>) o;
            }
        }else if (obj instanceof Page<?>) page = (Page<?>) obj;

//        if (id.endsWith("Page") && page == null) {
//            page=new Page<>();
//            page.setPageSize(pageSize);
//            page.setPageNo(pageNo);
//        }

        if (page!=null) {
            Connection connection = (Connection) invocation.getArgs()[0];
            String dbType = connection.getMetaData().getDatabaseProductName();
            String sql = boundSql.getSql();
            this.setTotalRecord(page,
                    mappedStatement, connection);
            String pageSql = this.getPageSql(page, sql,dbType);
            //利用反射设置当前BoundSql对应的sql属性为我们建立好的分页Sql语句
//            ReflectUtil.setFieldValue(boundSql, "sql", pageSql);
            metaObject.setValue("delegate.boundSql.sql", pageSql);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private String getPageSql(Page<?> page, String sql,String dbType) {
        StringBuffer sqlBuffer = new StringBuffer(sql);
        if ("mysql".equalsIgnoreCase(dbType)) {
            return getMysqlPageSql(page, sqlBuffer);
        } else if ("sqlServer".equalsIgnoreCase(dbType)) {
            return getSqlServerPageSql(page, sqlBuffer);
        } else if ("oracle".equalsIgnoreCase(dbType)) {
            return getOraclePageSql(page, sqlBuffer);
        }
        return sqlBuffer.toString();
    }


    private String getMysqlPageSql(Page<?> page, StringBuffer sqlBuffer) {
        //计算第一条记录的位置，Mysql中记录的位置是从0开始的。
        int offset = (page.getPageNo() - 1) * page.getPageSize();
        sqlBuffer.append(" limit ").append(offset).append(",").append(page.getPageSize());
        return sqlBuffer.toString();
    }


    private String getOraclePageSql(Page<?> page, StringBuffer sqlBuffer) {
        //计算第一条记录的位置，Oracle分页是通过rownum进行的，而rownum是从1开始的
        int offset = (page.getPageNo() - 1) * page.getPageSize() + 1;
        sqlBuffer.insert(0, "select u.*, rownum r from (").append(") u where rownum < ").append(offset + page.getPageSize());
        sqlBuffer.insert(0, "select * from (").append(") where r >= ").append(offset);
        //上面的Sql语句拼接之后大概是这个样子：
        //select * from (select u.*, rownum r from (select * from t_user) u where rownum < 31) where r >= 16
        return sqlBuffer.toString();
    }

    private String getSqlServerPageSql(Page<?> page, StringBuffer sqlBuffer) {
        // 计算第一条记录的位置，sqlServer中记录的位置是从0开始的。
        int offset = (page.getPageNo()) * page.getPageSize();

        // 先取where之前的部分,然后lastIndexOf来获取from的位置,防止字段名称中有"from"
        String frontPart = sqlBuffer.substring(0, sqlBuffer.toString().toLowerCase().indexOf("where") == -1 ? sqlBuffer.length() : sqlBuffer.toString().toLowerCase().indexOf("where"));
        int indexOfFrom = frontPart.toLowerCase().lastIndexOf("from");
        String selectFld = sqlBuffer.substring(0, indexOfFrom);

        // 取出 order by 语句
        int lastIndexOfOrderBy = sqlBuffer.toString().toLowerCase().lastIndexOf("order by");
        String orderby = sqlBuffer.substring(lastIndexOfOrderBy, sqlBuffer.length());

        // 取出 from 语句后的内容
        String selectFromTableAndWhere = sqlBuffer.substring(indexOfFrom, lastIndexOfOrderBy);

        // 清空sqlBuffer开始重新拼装
        sqlBuffer.delete(0, sqlBuffer.length());
        sqlBuffer.append("select * from (").append(selectFld).append(",ROW_NUMBER() OVER(").append(orderby).append(") as rownum ").append(selectFromTableAndWhere).append(" ) temp ").append(" where  rownum BETWEEN  ").append(offset).append(" and ").append((page.getPageNo() + 1) * page.getPageSize());
        return sqlBuffer.toString();
    }

    private void setTotalRecord(Page<?> page,
                                MappedStatement mappedStatement, Connection connection) {
        //获取对应的BoundSql，这个BoundSql其实跟我们利用StatementHandler获取到的BoundSql是同一个对象。
        //delegate里面的boundSql也是通过mappedStatement.getBoundSql(paramObj)方法获取到的。
        BoundSql boundSql = mappedStatement.getBoundSql(page);
        //获取到我们自己写在Mapper映射语句中对应的Sql语句
        String sql = boundSql.getSql();
        //通过查询Sql语句获取到对应的计算总记录数的sql语句
        String countSql = String.format("select count(1) from (%s) a", sql);
        //通过BoundSql获取对应的参数映射
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        //利用Configuration、查询记录数的Sql语句countSql、参数映射关系parameterMappings和参数对象page建立查询记录数对应的BoundSql对象。
        BoundSql countBoundSql = new BoundSql(mappedStatement.getConfiguration(), countSql, parameterMappings, page);
        //通过mappedStatement、参数对象page和BoundSql对象countBoundSql建立一个用于设定参数的ParameterHandler对象
        ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, page, countBoundSql);
        //通过connection建立一个countSql对应的PreparedStatement对象。

        try (PreparedStatement pstmt = connection.prepareStatement(countSql);
             ResultSet rs = pstmt.executeQuery()) {
            parameterHandler.setParameters(pstmt);
            if (rs.next()) {
                page.setCount(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


//    private static class ReflectUtil {
//        public static Object getFieldValue(Object obj, String fieldName) {
//            Object result = null;
//            Field field = ReflectUtil.getField(obj, fieldName);
//            if (field != null) {
//                field.setAccessible(true);
//                try {
//                    result = field.get(obj);
//                } catch (IllegalArgumentException | IllegalAccessException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            return result;
//        }
//
//        /**
//         * 利用反射获取指定对象里面的指定属性
//         *
//         * @param obj       目标对象
//         * @param fieldName 目标属性
//         * @return 目标字段
//         */
//        private static Field getField(Object obj, String fieldName) {
//            Field field = null;
//            for (Class<?> clazz = obj.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
//                try {
//                    field = clazz.getDeclaredField(fieldName);
//                    break;
//                } catch (NoSuchFieldException e) {
//                    //这里不用做处理，子类没有该字段可能对应的父类有，都没有就返回null。
//                }
//            }
//            return field;
//        }
//
//        /**
//         * 利用反射设置指定对象的指定属性为指定的值
//         *
//         * @param obj        目标对象
//         * @param fieldName  目标属性
//         * @param fieldValue 目标值
//         */
//        public static void setFieldValue(Object obj, String fieldName,
//                                         String fieldValue) {
//            Field field = ReflectUtil.getField(obj, fieldName);
//            if (field != null) {
//                try {
//                    field.setAccessible(true);
//                    field.set(obj, fieldValue);
//                } catch (IllegalArgumentException | IllegalAccessException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

}
