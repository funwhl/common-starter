package com.eighteen.common.feedback.datasource;

import com.eighteen.common.feedback.constants.DsConstants;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@UtilityClass
public class DataSourcePicker {
    public static String getDataSourceByClickType(String clickType) {
        if (StringUtils.isNotBlank(clickType)) {
            switch (clickType) {
                case "wxChannel":
                    return DsConstants.WEIXIN;
                case "sigmobChannel":
                    return DsConstants.SIGMOB;
                case "kuaishouChannel":
                    return DsConstants.KUAISHOU;
                case "toutiao":
                    return DsConstants.TOUTIAO;
                case "baiduChannel":
                    return DsConstants.BAIDU;
                case "gdtDir":
                    return DsConstants.GDT;
                default:
                    throw new IllegalArgumentException("无法通过clickType找到数据源：" + clickType);
            }
        }
        throw new IllegalArgumentException("无法通过clickType找到数据源：" + clickType);
    }

    public static String getDataSourceByActiveType(String activeType) {
        if (Lists.newArrayList(DsConstants.WEIXIN,DsConstants.SIGMOB, DsConstants.KUAISHOU, DsConstants.TOUTIAO, DsConstants.BAIDU,
                DsConstants.GDT).contains(activeType)) {
            return activeType;
        }
        throw new IllegalArgumentException("无activeType对应的数据源：" + activeType);
    }
}
