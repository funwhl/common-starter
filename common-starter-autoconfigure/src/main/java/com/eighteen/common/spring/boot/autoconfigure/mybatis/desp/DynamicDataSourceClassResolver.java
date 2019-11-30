
package com.eighteen.common.spring.boot.autoconfigure.mybatis.desp;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ibatis.binding.MapperProxy;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.beans.DirectFieldAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;


@Slf4j
public class DynamicDataSourceClassResolver {

    private static final String ADVISED_FIELD_NAME = "advised";
    private static final String
            CLASS_JDK_DYNAMIC_AOP_PROXY = "org.springframework.aop.framework.JdkDynamicAopProxy";
    private static boolean mpEnabled = false;
    private static Field mapperInterfaceField;

    static {
        Class<?> proxyClass = null;
        try {
            proxyClass = Class.forName("com.baomidou.mybatisplus.core.override.MybatisMapperProxy");
        } catch (ClassNotFoundException e1) {
            try {
                proxyClass = Class.forName("com.baomidou.mybatisplus.core.override.PageMapperProxy");
            } catch (ClassNotFoundException e2) {
                try {
                    proxyClass = Class.forName("org.apache.ibatis.binding.MapperProxy");
                } catch (ClassNotFoundException e3) {
                }
            }
        }
        if (proxyClass != null) {
            try {
                mapperInterfaceField = proxyClass.getDeclaredField("mapperInterface");
                mapperInterfaceField.setAccessible(true);
                mpEnabled = true;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    public static Class getTargetClass(Object candidate) {
        if (!org.springframework.aop.support.AopUtils.isJdkDynamicProxy(candidate)) {
            return org.springframework.aop.support.AopUtils.getTargetClass(candidate);
        }

        return getTargetClassFromJdkDynamicAopProxy(candidate);
    }

    private static Class getTargetClassFromJdkDynamicAopProxy(Object candidate) {
        try {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(candidate);
            if (!invocationHandler.getClass().getName().equals(CLASS_JDK_DYNAMIC_AOP_PROXY)) {
                //在目前的spring版本，这处永远不会执行，除非以后spring的dynamic proxy实现变掉
                log.warn("the invocationHandler of JdkDynamicProxy isn`t the instance of "
                        + CLASS_JDK_DYNAMIC_AOP_PROXY);
                return candidate.getClass();
            }
            AdvisedSupport advised = (AdvisedSupport) new DirectFieldAccessor(invocationHandler).getPropertyValue(ADVISED_FIELD_NAME);
            Class targetClass = advised.getTargetClass();
            if (Proxy.isProxyClass(targetClass)) {
                // 目标类还是代理，递归
                Object target = advised.getTargetSource().getTarget();
                return getTargetClassFromJdkDynamicAopProxy(target);
            }
            return targetClass;
        } catch (Exception e) {
            log.error("get target class from " + CLASS_JDK_DYNAMIC_AOP_PROXY + " error", e);
            return candidate.getClass();
        }
    }

    public Class<?> targetClass(MethodInvocation invocation) throws IllegalAccessException {
        if (mpEnabled) {
            Object target = invocation.getThis();
            Class<?> targetClass = target.getClass();

            if (Proxy.isProxyClass(targetClass)) {
                InvocationHandler handler = Proxy.getInvocationHandler(target);
//                if (handler instanceof MapperProxy)
//                    return (Class) mapperInterfaceField
//                            .get(Proxy.getInvocationHandler(target));
//                else {
//                    AdvisedSupport advised = (AdvisedSupport) new DirectFieldAccessor(handler).getPropertyValue(ADVISED_FIELD_NAME);
//                    Class ss = advised.getTargetClass();
//                }
            }
            return targetClass;
        }
        return invocation.getMethod().getDeclaringClass();
    }
}