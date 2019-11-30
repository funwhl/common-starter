package com.eighteen.common.spring.boot.autoconfigure.mybatis.desp.tem;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wangwei.
 * Date: 2019/9/14
 * Time: 21:51
 */

@Configuration
public class DataSourceAutoConfiguration {

    @Bean
    DataSourceAnnotationAsp dataSourceAnnotationAsp() {
        return new DataSourceAnnotationAsp();
    }
}
