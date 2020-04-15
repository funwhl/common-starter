package com.eighteen.common.spring.boot.autoconfigure.ds.dynamic;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DynamicDataSourceEntity implements HasDynamicDataSource {
    private String type;

    @Override
    public String getDataSource() {
        if(type=="wx"){
            return "wx";
        }
        else if(type=="toutiao"){
            return "toutiao";
        }
        return null;
    }
}
