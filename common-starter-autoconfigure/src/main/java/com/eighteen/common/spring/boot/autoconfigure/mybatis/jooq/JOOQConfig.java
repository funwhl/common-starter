package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:07
 */
public class JOOQConfig implements SpringApplicationRunListener {

    private SpringApplication application;

    public JOOQConfig(SpringApplication application, String[] args) {
        this.application = application;
    }

    @Override
    public void starting() {
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(application.getClassLoader()));
            CtClass clazz = pool.get("org.jooq.impl.AbstractRecord");
            CtMethod fromPojo = new CtMethod(pool.get("boolean"), "fromPojo", new CtClass[]{pool.get("java.lang.Object")}, clazz);
            fromPojo.setModifiers(Modifier.PUBLIC);
            fromPojo.setBody("{return false;}");
            clazz.addMethod(fromPojo);

            CtMethod method = clazz.getDeclaredMethod("from");
            method.insertBefore("if (this.fromPojo(source)) return;");

            clazz.toClass();

            clazz = pool.get("org.jooq.impl.DefaultRecordMapperProvider");
            method = clazz.getDeclaredMethod("provide");
            StringBuilder sb = new StringBuilder();
            sb.append("try {")
                    .append("return type.getMethod(\"createMapper\", new Class[]{ Class.class }).invoke(null, new Object[] {type});")
                    .append("} catch (Exception e) {")
                    .append("e.printStackTrace();")
                    .append("}");

            method.insertBefore(sb.toString());
            clazz.toClass();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

}

