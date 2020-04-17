package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.DynamicDataSourceAutoConfiguration;
import com.eighteen.common.spring.boot.autoconfigure.job.JobAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;

@SpringBootApplication(exclude = {
        DynamicDataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class, JooqAutoConfiguration.class,
        DruidDataSourceAutoConfigure.class, JobAutoConfiguration.class})
public class TestApplication {
}
