package com.eighteen.common.feedback.domain;

import com.eighteen.common.feedback.constants.DsConstants;

/**
 * 点击类型枚举
 *
 * @author lcomplete
 */

public enum ClickType {
    /**
     * 微信
     */
    WX(0, "wxChannel", DsConstants.WEIXIN),
    SIGMOB(1, "sigmobChannel", DsConstants.SIGMOB),
    KUAISHOU(2, "kuaishouChannel", DsConstants.KUAISHOU),
    TOUTIAO(3, "toutiao", DsConstants.TOUTIAO),
    BAIDU(4, "baiduChannel", DsConstants.BAIDU),
    GDT(5, "gdtDir", DsConstants.GDT),
    OPPO(6, "oppo", DsConstants.OPPO),
    QUTOUTIAO(7, "qutoutiao", DsConstants.QUTOUTIAO),
    SOHU(8, "sohu", DsConstants.SOHU),
    QIHU(9, "qihu", DsConstants.QIHU);

    private int id;
    private String type;
    private String dataSource;

    ClickType(int id, String type, String dataSource) {
        this.id = id;
        this.type = type;
        this.dataSource = dataSource;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDataSource() {
        return dataSource;
    }

    public static ClickType fromType(String type) {
        for (ClickType clickType : ClickType.values()) {
            if (clickType.getType().equals(type)) {
                return clickType;
            }
        }
        return null;
    }

    public static ClickType fromDataSource(String dataSource) {
        for (ClickType clickType : ClickType.values()) {
            if (clickType.getDataSource().equals(dataSource)) {
                return clickType;
            }
        }
        return null;
    }

    public static ClickType fromId(int id) {
        for (ClickType clickType : ClickType.values()) {
            if (clickType.getId() == id) {
                return clickType;
            }
        }
        return null;
    }
}
