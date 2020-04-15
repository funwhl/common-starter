package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsProcessor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 动态获取数据源处理器
 * @author lcomplete
 */
public class DsDynamicProcessor extends DsProcessor {
    /**
     * #dynamic
     */
    private static final String DYNAMIC_KEY = "#dynamic";

    @Override
    public boolean matches(String key) {
        return DYNAMIC_KEY.equals(key);
    }

    @Override
    public String doDetermineDatasource(MethodInvocation invocation, String key) {
        Object[] arguments = invocation.getArguments();
        //如果参数实现了HasDynamicDataSource接口，则从参数对象中获取数据源
        for (Object arg :
                arguments) {
            if (arg instanceof HasDynamicDataSource) {
                HasDynamicDataSource dynamicDataSource = (HasDynamicDataSource) arg;
                String dataSource = dynamicDataSource.getDataSource();
                if (dataSource != null) {
                    return dataSource;
                }
            }
        }
        return null;
    }
}
