package com.eighteen.common.feedback;


import com.eighteen.common.feedback.config.ClickThreadPoolConfig;
import com.eighteen.common.feedback.controller.ClickMonitorController;
import com.eighteen.common.feedback.controller.WarningController;
import com.eighteen.common.feedback.service.FeedbackErrorsService;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.feedback.service.impl.FeedbackErrorsServiceImpl;
import com.eighteen.common.feedback.service.impl.FeedbackServiceImpl;
import com.eighteen.common.spring.boot.autoconfigure.job.Job;
import com.eighteen.common.spring.boot.autoconfigure.job.JobAutoConfiguration;
import com.eighteen.common.spring.boot.autoconfigure.mybatis.autoconfigure.MybatisAutoConfiguration;
import com.eighteen.common.utils.FsService;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
    @PersistenceContext
    EntityManager em;
    private BeanFactory beanFactory;
    private AnnotationMetadata importingClassMetadata;
    private BeanDefinitionRegistry registry;

    @Bean
    FeedbackErrorsService feedbackErrorsService() {
        return new FeedbackErrorsServiceImpl();
    }

    @Bean
    FeedbackService feedbackService() {
        return new FeedbackServiceImpl(properties);
    }
//    @Bean
//    ClickThreadPoolConfig clickThreadPoolConfig() {
//        return new ClickThreadPoolConfig();
//    }
    @Bean
    ClickMonitorController clickMonitorController() {
        return new ClickMonitorController();
    }

    @Bean
    WarningController warningController() {
        return new WarningController();
    }

    @Bean
    JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
    @Bean
    FsService fsService() {
        return new FsService("cli_9e92953470729101", "wyhVGlfyOidIHUSLxxJJHcyhMzuHQ2lW");
    }

//    @Bean
//    @ConditionalOnExpression("${18.feedback.enable:true}")
//    @ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "mode", havingValue = "2")
//    SchedulingConfigurer schedule18Job() {
//        return taskRegistrar -> {
//            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_IMEI), properties.getCleanImeiCron());
//            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_ACTIVE), properties.getCleanActiveCron());
//            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_ACTIVE_HISTORY), properties.getCleanActiveHistoryCron());
//            taskRegistrar.addCronTask(() -> feedbackService().clean(CLEAN_CLICK), properties.getCleanClickCron());
//            taskRegistrar.addCronTask(() -> feedbackService().syncActive(null), properties.getSyncActiveCron());
//            taskRegistrar.addCronTask(() -> feedbackService().feedback(null), properties.getFeedbackCron());
////            taskRegistrar.addCronTask(() -> feedbackService().stat(STAT_DAY), properties.getDayStatCron());
//            // 次留存 定时任务
//            if (properties.getRetention())taskRegistrar.addCronTask(() -> feedbackService().secondStay(RETENTION), properties.getDayStatCron());
//
//        };
////        return new Scheduling18Configurer();
//    }

    @Bean
    @ConditionalOnExpression("${18.feedback.enable:true}")
    @ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "mode", havingValue = "1")
    Map<String, Job> simple18Jobs() {

        Map<String, Job> jobs = new HashMap<>();
        if (StringUtils.isNotBlank(properties.getCleanImeiCron()))jobs.put(CLEAN_IMEI.getKey(), Job.builder().jobName(CLEAN_IMEI.getKey()).cron(properties.getCleanImeiCron())
                .job(c -> feedbackService().clean(CLEAN_IMEI,c)).build());

        if (StringUtils.isNotBlank(properties.getCleanActiveCron()))jobs.put(CLEAN_ACTIVE.getKey(), Job.builder().jobName(CLEAN_ACTIVE.getKey()).cron(properties.getCleanActiveCron())
                .job(c -> feedbackService().clean(CLEAN_ACTIVE,c)).build());

        if (StringUtils.isNotBlank(properties.getCleanClickCron()))jobs.put(CLEAN_CLICK.getKey(), Job.builder().jobName(CLEAN_CLICK.getKey()).cron(properties.getCleanClickCron())
                .job(c -> feedbackService().clean(CLEAN_CLICK,c)).build());

        if (StringUtils.isNotBlank(properties.getCleanActiveHistoryCron()))jobs.put(CLEAN_ACTIVE_HISTORY.getKey(), Job.builder().jobName(CLEAN_ACTIVE_HISTORY.getKey()).cron(properties.getCleanActiveHistoryCron())
                .job(c -> feedbackService().clean(CLEAN_ACTIVE_HISTORY,c)).build());

        if (StringUtils.isNotBlank(properties.getSyncActiveCron())) {
            Job.JobBuilder builder = Job.builder().jobName(SYNC_ACTIVE.getKey()).cron(properties.getSyncActiveCron())
                    .job(c -> feedbackService().syncActive(c)).shardingTotalCount(properties.getSc());
            if (!properties.getScParam().equals("")) builder.shardingItemParameters(properties.getScParam());
            jobs.put(SYNC_ACTIVE.getKey(), builder.build());
        }

        if (StringUtils.isNotBlank(properties.getFeedbackCron())) {
            Job.JobBuilder builder = Job.builder().jobName(FEED_BACK.getKey()).cron(properties.getFeedbackCron())
                    .job(c -> feedbackService().feedback(c,false)).shardingTotalCount(properties.getSc());
            if (!properties.getScParam().equals("")) builder.shardingItemParameters(properties.getScParam());
            jobs.put(FEED_BACK.getKey(), builder.build());
        }

        if (StringUtils.isNotBlank(properties.getFeedbackColdCron())&&!properties.getColdData()) {
            Job.JobBuilder builder = Job.builder().jobName(FEED_BACK_COLD.getKey()).cron(properties.getFeedbackColdCron())
                    .job(c -> feedbackService().feedback(c,true)).shardingTotalCount(properties.getSc());
            if (!properties.getScParam().equals("")) builder.shardingItemParameters(properties.getScParam());
            jobs.put(FEED_BACK_COLD.getKey(), builder.build());
        }

//        jobs.put(STAT_DAY.getKey(), Job.builder().jobName(STAT_DAY.getKey()).cron(properties.getDayStatCron()).failover(true)
//                .job(c -> feedbackService().stat(STAT_DAY)).build());

        if (properties.getRetention()&&StringUtils.isNotBlank(properties.getCleanImeiCron()))
        jobs.put(RETENTION.getKey(), Job.builder().jobName(RETENTION.getKey()).cron(properties.getRetentionCron()).failover(true)
                .job(c -> feedbackService().secondStay(RETENTION,c)).monitorExecution(false).build());

        return jobs;
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();
//        restTemplate.setRequestFactory(new HttpComponentsClientRestfulHttpRequestFactory());
        return restTemplate;
    }

//    private static final class HttpComponentsClientRestfulHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
//        @Override
//        protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
//            if (httpMethod == HttpMethod.GET) {
//                return new HttpGetRequestWithEntity(uri);
//            }
//            return super.createHttpUriRequest(httpMethod, uri);
//        }
//    }
//
//    private static final class HttpGetRequestWithEntity extends HttpEntityEnclosingRequestBase {
//        public HttpGetRequestWithEntity(final URI uri) {
//            super.setURI(uri);
//        }
//
//        @Override
//        public String getMethod() {
//            return HttpMethod.GET.name();
//        }
//    }
}
