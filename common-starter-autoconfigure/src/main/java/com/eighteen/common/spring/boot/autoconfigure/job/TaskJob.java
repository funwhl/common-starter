package com.eighteen.common.spring.boot.autoconfigure.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 20:52
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface TaskJob {

    //    @NotNull
    String jobName();

    //    @NotNull
    String cron();

    //    @NotNull
    String desc();

    boolean failover() default false;

    String shardingItemParameters() default "";

    String jobParameter() default "";

    boolean overwrite() default true;

    int shardingTotalCount() default 1;

}