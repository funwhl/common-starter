package com.eighteen.common.spring.boot.autoconfigure.ds.processor;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Created by wangwei.
 * Date: 2020/4/14
 * Time: 16:00
 */
public class DsThreadProcessor extends DsProcessor {
    private static final String THREAD_PREFIX = "#thread";

    @Override
    public boolean matches(String key) {
        return key.startsWith(THREAD_PREFIX);
    }

    @Override
    public String doDetermineDatasource(MethodInvocation invocation, String key) {
        Thread thread = Thread.currentThread();
        return thread.getName().split("#")[1];
    }

}
