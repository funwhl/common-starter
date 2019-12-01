package com.eighteen.common.feedback;


import com.eighteen.common.feedback.controller.ClickMonitorController;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.feedback.service.impl.FeedbackServiceImpl;
import com.eighteen.common.spring.boot.autoconfigure.job.Job;
import com.eighteen.common.spring.boot.autoconfigure.job.JobAutoConfiguration;
import com.eighteen.common.spring.boot.autoconfigure.mybatis.autoconfigure.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

import java.util.HashMap;
import java.util.Map;

import static com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType.*;


/**
 * Created by eighteen.
 * Date: 2019/8/25
 * Time: 11:20
 */

@Configuration
@ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "channel")
@EnableConfigurationProperties(EighteenProperties.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@AutoConfigureBefore(JobAutoConfiguration.class)
@EnableScheduling
public class FeedbackAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger(FeedbackAutoConfiguration.class);
    @Autowired
    EighteenProperties properties;
    private BeanFactory beanFactory;
    private AnnotationMetadata importingClassMetadata;
    private BeanDefinitionRegistry registry;

    @Bean
    FeedbackService feedbackService() {
        return new FeedbackServiceImpl();
    }

    @Bean
    ClickMonitorController clickMonitorController() {
        return new ClickMonitorController();
    }

    @Bean
    @ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "mode", havingValue = "2")
    SchedulingConfigurer schedule18Job() {
        return taskRegistrar -> {
            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_IMEI), properties.getCleanImeiCron());
            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_ACTIVE), properties.getCleanActiveCron());
            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_CLICK), properties.getCleanClickCron());
            taskRegistrar.addCronTask(() -> feedbackService().syncActive(), properties.getSyncActiveCron());
            taskRegistrar.addCronTask(() -> feedbackService().feedback(), properties.getFeedbackCron());
            taskRegistrar.addCronTask(() -> feedbackService().stat(STAT_DAY), properties.getDayStatCron());
            // 次留存 定时任务
            taskRegistrar.addCronTask(() -> feedbackService().secondStay(RETENTION), properties.getDayStatCron());

        };
//        return new Scheduling18Configurer();
    }

    @Bean
    @ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "mode", havingValue = "1")
    Map<String, Job> simple18Jobs() {

        Map<String, Job> jobs = new HashMap<>();
        jobs.put(CLEAN_IMEI.getKey(), Job.builder().jobName(CLEAN_IMEI.getKey()).cron(properties.getCleanImeiCron()).failover(true)
                .job(c -> feedbackService().clean(CLEAN_IMEI)).build());

        jobs.put(CLEAN_ACTIVE.getKey(), Job.builder().jobName(CLEAN_ACTIVE.getKey()).cron(properties.getCleanActiveCron()).failover(true)
                .job(c -> feedbackService().clean(CLEAN_ACTIVE)).build());

        jobs.put(CLEAN_CLICK.getKey(), Job.builder().jobName(CLEAN_CLICK.getKey()).cron(properties.getCleanClickCron()).failover(true)
                .job(c -> feedbackService().clean(CLEAN_CLICK)).build());

        jobs.put(SYNC_ACTIVE.getKey(), Job.builder().jobName(SYNC_ACTIVE.getKey()).cron(properties.getSyncActiveCron()).failover(true)
                .job(c -> feedbackService().syncActive()).monitorExecution(false).build());

        jobs.put(FEED_BACK.getKey(), Job.builder().jobName(FEED_BACK.getKey()).cron(properties.getFeedbackCron()).failover(true)
                .job(c -> feedbackService().feedback()).monitorExecution(false).build());

        jobs.put(STAT_DAY.getKey(), Job.builder().jobName(STAT_DAY.getKey()).cron(properties.getDayStatCron()).failover(true)
                .job(c -> feedbackService().stat(STAT_DAY)).build());

        jobs.put(RETENTION.getKey(), Job.builder().jobName(RETENTION.getKey()).cron(properties.getRetentionCron()).failover(true)
                .job(c -> feedbackService().secondStay(RETENTION)).monitorExecution(false).build());

        return jobs;
    }
}
