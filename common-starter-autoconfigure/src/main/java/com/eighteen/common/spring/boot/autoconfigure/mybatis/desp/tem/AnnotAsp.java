package com.eighteen.common.spring.boot.autoconfigure.mybatis.desp.tem;

import com.eighteen.common.annotation.DS;
import com.eighteen.common.spring.boot.autoconfigure.desp.DynamicDataSourceClassResolver;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.toolkit.DynamicDataSourceContextHolder;
import lombok.Setter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AdviceName;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * Created by wangwei.
 * Date: 2019/10/4
 * Time: 1:34
 */
@Aspect
@Order(-2147483647)
public class AnnotAsp  {
    private static final String DYNAMIC_PREFIX = "#";
    private static final DynamicDataSourceClassResolver RESOLVER = new DynamicDataSourceClassResolver();
    @Setter
    private DsProcessor dsProcessor;


    @Pointcut("@within(com.eighteen.common.annotation.DS)"
            + " || @annotation(com.eighteen.common.annotation.DS)"
    )
    private void dataSourceAspect() {
    }


    @Before("dataSourceAspect()")
    public void requestLimit( JoinPoint joinPoint) {
        try {
            DynamicDataSourceContextHolder.push(determineDatasource(joinPoint));

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private String determineDatasource(JoinPoint joinPoint) {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        DS ds = method.isAnnotationPresent(DS.class)
                ? method.getAnnotation(DS.class)
                : AnnotationUtils.findAnnotation(joinPoint.getThis().getClass(), DS.class);
        String key = ds.value();
//        return (!key.isEmpty() && key.startsWith(DYNAMIC_PREFIX)) ? dsProcessor
//                .determineDatasource(invocation, key) : key;
        return key;
    }

}
