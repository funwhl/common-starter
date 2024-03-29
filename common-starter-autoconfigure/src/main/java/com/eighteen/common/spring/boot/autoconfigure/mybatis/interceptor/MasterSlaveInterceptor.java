package com.eighteen.common.spring.boot.autoconfigure.mybatis.interceptor;///**
// * Copyright © 2018 organization baomidou
// * <pre>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * <pre/>
// */
//package com.eighteen.common.spring.boot.autoconfigure.mybatis.interceptor;
//
//import com.eighteen.common.spring.boot.autoconfigure.ds.toolkit.DynamicDataSourceContextHolder;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.ibatis.executor.Executor;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.SqlCommandType;
//import org.apache.ibatis.plugin.*;
//import org.apache.ibatis.session.ResultHandler;
//import org.apache.ibatis.session.RowBounds;
//import org.springframework.util.StringUtils;
//
//import java.util.Properties;
//
///**
// * Master-slave Separation Plugin with mybatis
// *
// * @author TaoYu
// * @since 2.5.1
// */
//@Intercepts({
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
//                RowBounds.class, ResultHandler.class}),
//        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class,
//                Object.class})})
//@Slf4j
//public class MasterSlaveInterceptor implements Interceptor {
//
//    private static final String MASTER = "master";
//
//    private static final String SLAVE = "slave";
//
//    @Override
//    public Object intercept(Invocation invocation) throws Throwable {
//        Object[] args = invocation.getArgs();
//        MappedStatement ms = (MappedStatement) args[0];
//        try {
//            if (StringUtils.isEmpty(DynamicDataSourceContextHolder.peek())) {
//                DynamicDataSourceContextHolder
//                        .push(SqlCommandType.SELECT == ms.getSqlCommandType() ? SLAVE : MASTER);
//            }
//            return invocation.proceed();
//        } finally {
//            DynamicDataSourceContextHolder.clear();
//        }
//    }
//
//    @Override
//    public Object plugin(Object target) {
//        return target instanceof Executor ? Plugin.wrap(target, this) : target;
//    }
//
//    @Override
//    public void setProperties(Properties properties) {
//    }
//}
