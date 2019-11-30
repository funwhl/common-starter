package com.eighteen.common.spring.boot.autoconfigure.desp;

import com.eighteen.common.spring.boot.autoconfigure.ds.toolkit.DynamicDataSourceContextHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Created by wangwei.
 * Date: 2019/10/4
 * Time: 4:25
 */
public class MyDataSource extends org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.peek();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        return super.determineTargetDataSource();
    }

    public DataSource getDataSource(String ds) {
//        if (StringUtils.isEmpty(ds)) {
//            return determinePrimaryDataSource();
//        } else if (!groupDataSources.isEmpty() && groupDataSources.containsKey(ds)) {
//            log.debug("dynamic-datasource switch to the datasource named [{}]", ds);
//            return groupDataSources.get(ds).determineDataSource();
//        } else if (dataSourceMap.containsKey(ds)) {
//            log.debug("dynamic-datasource switch to the datasource named [{}]", ds);
//            return dataSourceMap.get(ds);
//        }
//        if (strict) {
//            throw new RuntimeException("dynamic-datasource could not find a datasource named" + ds);
//        }
        return determineTargetDataSource();
    }
}
