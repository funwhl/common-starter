package com.eighteen.common.spring.boot.autoconfigure.desp.tem;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * Created by wangwei.
 * Date: 2019/9/15
 * Time: 12:51
 */
@Aspect
public class DataSourceAnnotationAsp implements Ordered {
    @Pointcut("@within(com.eighteen.common.spring.boot.autoconfigure.desp.tem.DataSource)"
            + " || @annotation(com.eighteen.common.spring.boot.autoconfigure.desp.tem.DataSource)"
    )
    private void dataSourceAspect() {
    }

    @Before("dataSourceAspect()")
    public void requestLimit(final JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        boolean flag = method.isAnnotationPresent(DataSource.class);
        if (flag) {
            DataSource dataSource = method.getAnnotation(DataSource.class);
            System.out.println(dataSource.value());
        } else {
            DataSource classAnnotation = AnnotationUtils.findAnnotation(methodSignature.getMethod().getDeclaringClass(), DataSource.class);
            if (classAnnotation != null) {
                System.out.println("class" + classAnnotation.value());
            } else {
                System.out.println(111);
            }
        }

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
