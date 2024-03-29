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
package com.eighteen.common.spring.boot.autoconfigure.ds.provider;

import com.eighteen.common.spring.boot.autoconfigure.ds.DynamicDataSourceCreator;
import com.eighteen.common.spring.boot.autoconfigure.ds.autoconfigure.DataSourceProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public abstract class AbstractDataSourceProvider implements DynamicDataSourceProvider {

    @Autowired
    private DynamicDataSourceCreator dynamicDataSourceCreator;

    protected Map<String, DataSource> createDataSourceMap(
            Map<String, DataSourceProperty> dataSourcePropertiesMap) {
        Map<String, DataSource> dataSourceMap = new HashMap<>(dataSourcePropertiesMap.size() * 2);
        for (Map.Entry<String, DataSourceProperty> item : dataSourcePropertiesMap.entrySet()) {
            String pollName = item.getKey();
            DataSourceProperty dataSourceProperty = item.getValue();
            dataSourceProperty.setPollName(pollName);
            dataSourceMap.put(pollName, dynamicDataSourceCreator.createDataSource(dataSourceProperty));
        }
        return dataSourceMap;
    }
}
