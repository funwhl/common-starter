package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 指定动态数据源
 * 可以放在Mapper方法的参数中以达到动态设置数据源的效果
 * @author lcomplete
 */
@Data
@AllArgsConstructor
public class SpecifyDynamicDataSource implements HasDynamicDataSource {
    private String dataSource;
}
