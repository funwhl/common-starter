package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

/**
 * 是否存在动态数据源
 * @author lcomplete
 */
public interface HasDynamicDataSource {

    /**
     * @return 数据源
     */
    String getDataSource();

}
