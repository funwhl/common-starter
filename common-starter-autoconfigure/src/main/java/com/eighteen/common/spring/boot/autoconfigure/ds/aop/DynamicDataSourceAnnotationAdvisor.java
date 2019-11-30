package com.eighteen.common.spring.boot.autoconfigure.ds.aop;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.toolkit.DynamicDataSourceContextHolder;
import lombok.Setter;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;


public class DynamicDataSourceAnnotationAdvisor extends AbstractPointcutAdvisor implements
        BeanFactoryAware {

    private static final long serialVersionUID = 173291046153652655L;

    /**
     * The identification of SPEL.
     */
    private static final String DYNAMIC_PREFIX = "#";
    //    private static final DynamicDataSourceClassResolver RESOLVER = new DynamicDataSourceClassResolver();
    @Setter
    private DsProcessor dsProcessor;

    @Override
    public Pointcut getPointcut() {
        Pointcut cpc = new AnnotationMatchingPointcut(DS.class, true);
        Pointcut mpc = AnnotationMatchingPointcut.forMethodAnnotation(DS.class);
        return new ComposablePointcut(cpc).union(mpc);
    }

    @Override
    public Advice getAdvice() {
        return (MethodInterceptor) invocation -> {
            try {
                DynamicDataSourceContextHolder.push(determineDatasource(invocation));
                return invocation.proceed();
            } finally {
                DynamicDataSourceContextHolder.poll();
            }
        };
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (getAdvice() instanceof BeanFactoryAware) {
            ((BeanFactoryAware) getAdvice()).setBeanFactory(beanFactory);
        }
    }

    private String determineDatasource(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        DS ds = method.isAnnotationPresent(DS.class)
                ? method.getAnnotation(DS.class)
                : AnnotationUtils.findAnnotation(invocation.getThis().getClass(), DS.class);
        String key = ds.value();
        return (!key.isEmpty() && key.startsWith(DYNAMIC_PREFIX)) ? dsProcessor
                .determineDatasource(invocation, key) : key;
    }
}
