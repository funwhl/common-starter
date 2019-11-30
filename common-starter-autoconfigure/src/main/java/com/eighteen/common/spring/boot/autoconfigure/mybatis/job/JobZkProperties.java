package com.eighteen.common.spring.boot.autoconfigure.mybatis.job;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.eighteen.common.spring.boot.autoconfigure.job.JobZkProperties.PREFIX;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 20:40
 */
@ConfigurationProperties(prefix = JobZkProperties.PREFIX)
@Getter
@Setter
public class JobZkProperties implements InitializingBean {
    public static final String PREFIX = "job";

    @Value("${spring.application.name}")
    private String appName;

    private String serverlist;

    private String namespace = "18-feedback-job";

    private int baseSleepTimeMilliseconds = 1000;

    private int maxSleepTimeMilliseconds = 3000;

    private int maxRetries = 1;

    private int sessionTimeOutMillseconds = 40000;

    private boolean saveLog = false;

    @Override
    public void afterPropertiesSet() {
        this.namespace = String.format("%s-%s", namespace, appName);
    }
}
