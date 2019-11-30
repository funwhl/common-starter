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
package com.eighteen.common.spring.boot.autoconfigure.mybatis.desp.tem;

import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.DataSourceProperty;
import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.druid.DruidConfig;
import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.hikari.HikariCpConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DynamicDataSourceProperties
 *
 * @author TaoYu Kanyuxia
 * @see DataSourceProperties
 * @since 1.0.0
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "18.datasource")
public class DynamicDataSourceProperties {

    private Map<String, DataSourceProperty> datasource = new LinkedHashMap<>();

    @NestedConfigurationProperty
    private DruidConfig druid = new DruidConfig();

    @NestedConfigurationProperty
    private HikariCpConfig hikari = new HikariCpConfig();

}
