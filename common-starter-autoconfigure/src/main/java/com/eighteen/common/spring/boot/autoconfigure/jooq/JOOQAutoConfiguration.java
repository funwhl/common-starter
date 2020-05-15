package com.eighteen.common.spring.boot.autoconfigure.jooq;//package com.eighteen.common.spring.boot.autoconfigure.jooq;
//
//import com.eighteen.common.spring.boot.autoconfigure.job.JobZkProperties;
//import org.springframework.boot.autoconfigure.AutoConfigureAfter;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import javax.sql.DataSource;
//
///**
// * Created by eighteen.
// * Date: 2019/9/1
// * Time: 12:25
// */
//
//@Configuration
//@ConditionalOnSingleCandidate(DataSource.class)
//@AutoConfigureAfter(DataSourceAutoConfiguration.class)
//@ConditionalOnProperty(prefix = "jooq", name = "enable")
//public class JOOQAutoConfiguration {
//    @Bean
//    public CommonDaoAutowireCandidateResolver autowireCandidateResolver() {
//        return new CommonDaoAutowireCandidateResolver();
//    }
//}
