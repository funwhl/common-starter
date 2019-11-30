package com.eighteen.common.spring.boot.autoconfigure.desp.tem;

import java.lang.annotation.*;

/**
 * Created by wangwei.
 * Date: 2019/9/15
 * Time: 11:33
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    String value() default "";
}
