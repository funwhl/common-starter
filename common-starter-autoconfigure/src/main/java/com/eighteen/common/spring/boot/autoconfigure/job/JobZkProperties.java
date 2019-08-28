package com.eighteen.common.spring.boot.autoconfigure.job;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.eighteen.common.spring.boot.autoconfigure.job.JobZkProperties.PREFIX;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 20:40
 */
@ConfigurationProperties(prefix = PREFIX)
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

    public int getSessionTimeOutMillseconds() {
        return sessionTimeOutMillseconds;
    }

    public void setSessionTimeOutMillseconds(int sessionTimeOutMillseconds) {
        this.sessionTimeOutMillseconds = sessionTimeOutMillseconds;
    }

    public String getServerlist() {
        return serverlist;
    }

    public void setServerlist(String serverlist) {
        this.serverlist = serverlist;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getBaseSleepTimeMilliseconds() {
        return baseSleepTimeMilliseconds;
    }

    public void setBaseSleepTimeMilliseconds(int baseSleepTimeMilliseconds) {
        this.baseSleepTimeMilliseconds = baseSleepTimeMilliseconds;
    }

    public int getMaxSleepTimeMilliseconds() {
        return maxSleepTimeMilliseconds;
    }

    public void setMaxSleepTimeMilliseconds(int maxSleepTimeMilliseconds) {
        this.maxSleepTimeMilliseconds = maxSleepTimeMilliseconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public void afterPropertiesSet() {
        this.namespace = String.format("%s-%s", namespace, appName);
    }
}
