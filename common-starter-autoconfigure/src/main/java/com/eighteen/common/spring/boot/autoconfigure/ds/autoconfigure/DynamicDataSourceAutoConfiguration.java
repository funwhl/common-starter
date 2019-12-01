/**
 * Copyright © 2018 organization baomidou
 * <pre>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <pre/>
 */
package com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure;

import com.eighteen.common.spring.boot.autoconfigure.ds.DynamicDataSourceConfigure;
import com.eighteen.common.spring.boot.autoconfigure.ds.DynamicDataSourceCreator;
import com.eighteen.common.spring.boot.autoconfigure.ds.DynamicRoutingDataSource;
import com.eighteen.common.spring.boot.autoconfigure.ds.aop.DynamicDataSourceAdvisor;
import com.eighteen.common.spring.boot.autoconfigure.ds.aop.DynamicDataSourceAnnotationAdvisor;
import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.druid.DruidDynamicDataSourceConfiguration;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsHeaderProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsSessionProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.processor.DsSpelExpressionProcessor;
import com.eighteen.common.spring.boot.autoconfigure.ds.provider.DynamicDataSourceProvider;
import com.eighteen.common.spring.boot.autoconfigure.ds.provider.YmlDynamicDataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicDataSourceProperties.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(DruidDynamicDataSourceConfiguration.class)
public class DynamicDataSourceAutoConfiguration {

    @Autowired
    private DynamicDataSourceProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        return new YmlDynamicDataSourceProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceCreator dynamicDataSourceCreator() {
        DynamicDataSourceCreator dynamicDataSourceCreator = new DynamicDataSourceCreator();
        dynamicDataSourceCreator.setDruidGlobalConfig(properties.getDruid());
        dynamicDataSourceCreator.setHikariGlobalConfig(properties.getHikari());
        dynamicDataSourceCreator.setGlobalPublicKey(properties.getPublicKey());
        return dynamicDataSourceCreator;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DynamicDataSourceProvider dynamicDataSourceProvider) {
        DynamicRoutingDataSource dataSource = new DynamicRoutingDataSource();
        dataSource.setPrimary(properties.getPrimary());
        dataSource.setStrategy(properties.getStrategy());
        dataSource.setProvider(dynamicDataSourceProvider);
        dataSource.setP6spy(properties.getP6spy());
        dataSource.setStrict(properties.getStrict());
        return dataSource;
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceAnnotationAdvisor dynamicDatasourceAnnotationAdvisor(
            DsProcessor dsProcessor) {
        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor();
        advisor.setDsProcessor(dsProcessor);
        advisor.setOrder(properties.getOrder());
        return advisor;
    }


//    @Bean
//    public AnnotAsp annotAsp() {
//        return new AnnotAsp();
//    }

    @Bean
    @ConditionalOnMissingBean
    public DsProcessor dsProcessor() {
        DsHeaderProcessor headerProcessor = new DsHeaderProcessor();
        DsSessionProcessor sessionProcessor = new DsSessionProcessor();
        DsSpelExpressionProcessor spelExpressionProcessor = new DsSpelExpressionProcessor();
        headerProcessor.setNextProcessor(sessionProcessor);
        sessionProcessor.setNextProcessor(spelExpressionProcessor);
        return headerProcessor;
    }

    @Bean
    @ConditionalOnBean(DynamicDataSourceConfigure.class)
    public DynamicDataSourceAdvisor dynamicAdvisor(
            DynamicDataSourceConfigure dynamicDataSourceConfigure, DsProcessor dsProcessor) {
        DynamicDataSourceAdvisor advisor = new DynamicDataSourceAdvisor(
                dynamicDataSourceConfigure.getMatchers());
        advisor.setDsProcessor(dsProcessor);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return advisor;
    }
}
