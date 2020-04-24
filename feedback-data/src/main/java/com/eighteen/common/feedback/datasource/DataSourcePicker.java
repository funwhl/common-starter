package com.eighteen.common.feedback.datasource;

import com.eighteen.common.feedback.constants.DsConstants;
import com.eighteen.common.feedback.domain.ClickType;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@UtilityClass
public class DataSourcePicker {

    /**
     * 根据点击类型获取数据源
     * @param clickType
     * @return
     */
    public static String getDataSourceByClickType(String clickType) {
        ClickType enumClickType = ClickType.fromType(clickType);
        if (enumClickType == null) {
            throw new IllegalArgumentException("无法通过clickType找到数据源：" + clickType);
        }
        return enumClickType.getDataSource();
    }

    /**
     * 获取激活类型对应的数据源
     *
     * @param activeType
     * @return 无对应的数据源时返回null
     */
    public static String getDataSourceByActiveType(String activeType) {
        if (Lists.newArrayList(DsConstants.WEIXIN, DsConstants.SIGMOB, DsConstants.KUAISHOU, DsConstants.TOUTIAO, DsConstants.BAIDU,
                DsConstants.GDT).contains(activeType)) {
            return activeType;
        }
        return null;
    }
}
