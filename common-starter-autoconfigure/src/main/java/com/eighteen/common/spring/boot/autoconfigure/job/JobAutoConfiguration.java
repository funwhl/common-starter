package com.eighteen.common.spring.boot.autoconfigure.job;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringValueResolver;

import java.util.Map;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 20:44
 */
@Configuration
@EnableConfigurationProperties(JobZkProperties.class)
@ConditionalOnProperty(prefix = JobZkProperties.PREFIX, name = "serverlist")
public class JobAutoConfiguration implements EmbeddedValueResolverAware {
    private static final Logger logger = LoggerFactory.getLogger(JobAutoConfiguration.class);
    private JobZkProperties jobZkProperties;
    //    private ApplicationContext applicationContext;
    private StringValueResolver resolver;
    @Value("${spring.application.name}")
    private String appName;

    public JobAutoConfiguration(JobZkProperties jobZkProperties) {
        System.out.println(jobZkProperties.toString());
        this.jobZkProperties = jobZkProperties;
    }

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter zookeeperRegistryCenter() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(jobZkProperties.getServerlist()
                , jobZkProperties.getNamespace());
        zookeeperConfiguration.setBaseSleepTimeMilliseconds(jobZkProperties.getBaseSleepTimeMilliseconds());
        zookeeperConfiguration.setMaxSleepTimeMilliseconds(jobZkProperties.getMaxSleepTimeMilliseconds());
        zookeeperConfiguration.setMaxRetries(jobZkProperties.getMaxRetries());
        zookeeperConfiguration.setSessionTimeoutMilliseconds(jobZkProperties.getSessionTimeOutMillseconds());
        return new ZookeeperRegistryCenter(zookeeperConfiguration);
    }

    @Bean
    @ConditionalOnBean(value = {ZookeeperRegistryCenter.class})
    public RegisterJobs registerJobs(ApplicationContext applicationContext, ZookeeperRegistryCenter zookeeperRegistryCenter) {
        return new RegisterJobs(applicationContext, zookeeperRegistryCenter);
    }

    private LiteJobConfiguration getJobConfiguration(TaskJob taskJob, Object object) {
        String c = resolver.resolveStringValue(taskJob.cron());
        String jobName = taskJob.jobName() + appName;
//        Optional.ofNullable(taskJob.desc()).orElseThrow(() -> new IllegalArgumentException("The desc cannot be null !"));

        SimpleJobConfiguration simpleJobConfiguration = new SimpleJobConfiguration(
                JobCoreConfiguration
                        .newBuilder(jobName, c, taskJob.shardingTotalCount())
                        .shardingItemParameters(org.apache.commons.lang3.StringUtils.isEmpty(taskJob.shardingItemParameters()) ? null : taskJob.shardingItemParameters())
                        .description(taskJob.desc())
                        .failover(taskJob.failover())
                        .jobParameter(org.apache.commons.lang3.StringUtils.isEmpty(taskJob.jobParameter()) ? null : taskJob.jobParameter())
                        .build(),
                object.getClass().getName());

        return LiteJobConfiguration
                .newBuilder(simpleJobConfiguration).overwrite(taskJob.overwrite())
                .maxTimeDiffSeconds(10)
                .monitorExecution(true)
                .build();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    private class RegisterJobs {
        public RegisterJobs(ApplicationContext applicationContext, ZookeeperRegistryCenter zookeeperRegistryCenter) {
            Map<String, Object> registerJobs = applicationContext.getBeansWithAnnotation(TaskJob.class);
            for (Map.Entry<String, Object> entry : registerJobs.entrySet()) {
                try {
                    Object object = entry.getValue();
                    if (!(object instanceof ElasticJob)) {
                        throw new ClassCastException("[" + object.getClass().getName() + "] The class type is not com.dangdang.ddframe.job.api.ElasticJob");
                    }
                    TaskJob taskJob = AnnotationUtils.findAnnotation(object.getClass(), TaskJob.class);
                    SpringJobScheduler springJobScheduler = new SpringJobScheduler(
                            (ElasticJob) object,
                            zookeeperRegistryCenter,
                            getJobConfiguration(taskJob, object)
                    );
                    springJobScheduler.init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
