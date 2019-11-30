package com.eighteen.base.annotation;

import java.lang.annotation.*;

/**
 * Created by wangwei.
 * Date: 2019/10/4
 * Time: 10:32
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /**
     * groupName or specific database name or spring SPEL name.
     *
     * @return the database you want to switch
     */
    String value();
}
