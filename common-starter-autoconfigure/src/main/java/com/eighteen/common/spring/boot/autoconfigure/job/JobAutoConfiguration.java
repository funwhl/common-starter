package com.eighteen.common.spring.boot.autoconfigure.job;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.context.support.GenericWebApplicationContext;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 20:44
 */
@Configuration
@EnableConfigurationProperties(JobZkProperties.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnProperty(prefix = JobZkProperties.PREFIX, name = "serverlist")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class JobAutoConfiguration implements EmbeddedValueResolverAware {
    private static final Logger logger = LoggerFactory.getLogger(JobAutoConfiguration.class);
    private JobZkProperties jobZkProperties;
    //    private ApplicationContext applicationContext;
    private StringValueResolver resolver;
    @Value("${spring.application.name}")
    private String appName;

    public JobAutoConfiguration(JobZkProperties jobZkProperties) {
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

    @Bean
    public JobEventConfiguration jobEventConfiguration(DataSource dataSource) {
        return new JobEventRdbConfiguration(dataSource);
    }

    private LiteJobConfiguration getJobConfiguration(Job job) {
        String c = resolver.resolveStringValue(job.getCron());
        String jobName = appName + "#" + job.getJobName();


        SimpleJobConfiguration simpleJobConfiguration = new SimpleJobConfiguration(
                JobCoreConfiguration
                        .newBuilder(jobName, c, job.getShardingTotalCount())
                        .shardingItemParameters(StringUtils.isEmpty(job.getShardingItemParameters())
                                ? null : job.getShardingItemParameters())
                        .description(job.getDescription())
                        .failover(job.isFailover())
                        .jobParameter(StringUtils.isEmpty(job.getJobParameter())
                                ? null : job.getJobParameter())
                        .build(),
                job.getClass().getName());

        return LiteJobConfiguration
                .newBuilder(simpleJobConfiguration).overwrite(job.isOverwrite())
                .maxTimeDiffSeconds(job.getMaxTimeDiffSeconds())
                .monitorExecution(job.isMonitorExecution())
                .build();
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    private class RegisterJobs {
        public RegisterJobs(ApplicationContext applicationContext, ZookeeperRegistryCenter zookeeperRegistryCenter) {
            Map<String, Object> registerJobs = applicationContext.getBeansWithAnnotation(TaskJob.class);
            if (applicationContext.containsBean("simple18Jobs")) {
                Map<String, Job> simpleJobs = (Map<String, Job>) applicationContext.getBean("simple18Jobs");
                registerJobs.putAll(simpleJobs);
              if (applicationContext instanceof AnnotationConfigServletWebServerApplicationContext) ((AnnotationConfigServletWebServerApplicationContext) applicationContext).getDefaultListableBeanFactory().removeBeanDefinition("simple18Jobs");
              if (applicationContext instanceof GenericWebApplicationContext)   ((GenericWebApplicationContext) applicationContext).getDefaultListableBeanFactory().removeBeanDefinition("simple18Jobs");
            }
            for (Map.Entry<String, Object> entry : registerJobs.entrySet()) {
                try {
                    Object object = entry.getValue();
                    SpringJobScheduler springJobScheduler;
                    if (object instanceof Job) {
                        Job job = (Job) object;
                        ElasticJob elasticJob = job.getJob();
                        springJobScheduler = new SpringJobScheduler(
                                elasticJob,
                                zookeeperRegistryCenter,
                                getJobConfiguration(job)
                        );
                    } else {
                        TaskJob taskJob = AnnotationUtils.findAnnotation(object.getClass(), TaskJob.class);
                        springJobScheduler = new SpringJobScheduler(
                                (ElasticJob) object,
                                zookeeperRegistryCenter,

                                getJobConfiguration(Job.builder()
                                        .jobName(taskJob.jobName())
                                        .failover(false)
                                        .cron(taskJob.cron())
                                        .overwrite(taskJob.overwrite())
                                        .description(taskJob.desc())
                                        .shardingTotalCount(taskJob.shardingTotalCount())
                                        .shardingItemParameters(taskJob.shardingItemParameters()).build())
                        );
                    }
                    springJobScheduler.init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
