package com.eighteen.common.spring.boot.autoconfigure.feedback;

import com.eighteen.common.spring.boot.autoconfigure.feedback.jobs.*;
import com.eighteen.common.spring.boot.autoconfigure.mybatis.ref.MybatisAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Created by eighteen.
 * Date: 2019/8/25
 * Time: 1:20
 */

@Configuration
@EnableConfigurationProperties(EighteenProperties.class)
@ConditionalOnProperty(prefix = EighteenProperties.PREFIX, name = "channel")
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class FeedbackAutoConfiguration  {
    public static final Logger logger = LoggerFactory.getLogger(FeedbackAutoConfiguration.class);
    private EighteenProperties eighteenProperties;
    private BeanFactory beanFactory;
    private AnnotationMetadata importingClassMetadata;
    private BeanDefinitionRegistry registry;

    public FeedbackAutoConfiguration(EighteenProperties eighteenProperties) {
        logger.info(eighteenProperties.toString());
        this.eighteenProperties = eighteenProperties;
    }

    @Bean
    CleanImeis cleanImeis() {
        logger.info("create job cleanImeis");
        return new CleanImeis();
    }

    @Bean
    CleanClickLog cleanClickLog() {
        logger.info("create job CleanClickLog");
        return new CleanClickLog();
    }


    @Bean
    FeedbackJob feedbackJob() {
        logger.info("create job FeedbackJob");
        return new FeedbackJob();
    }

    @Bean
    SyncActiveThirdJob syncActiveThirdJob() {
        logger.info("create job SyncActiveThirdJob");
        return new SyncActiveThirdJob();
    }

    @Bean
    TransferActiveT2History transferActiveT2History() {
        logger.info("create job TransferActiveT2History");
        return new TransferActiveT2History();
    }


}
