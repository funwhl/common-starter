package com.eighteen.common.feedback.domain;

import com.eighteen.common.feedback.constants.DsConstants;

/**
 * 点击类型枚举
 * @author admin
 */

public enum ClickType {
    /**
     * 微信
     */
    WX(0,"wxChannel", DsConstants.WEIXIN),
    SIGMOB(1,"sigmobChannel", DsConstants.SIGMOB),
    KUAISHOU(2,"kuaishouChannel",DsConstants.KUAISHOU),
    TOUTIAO(3,"toutiao",DsConstants.TOUTIAO),
    BAIDU(4,"baiduChannel",DsConstants.BAIDU),
    GDT(5,"gdtDir",DsConstants.GDT);

    private int id;
    private String type;
    private String dataSource;

    ClickType(int id, String type, String dataSource){
        this.id=id;
        this.type=type;
        this.dataSource=dataSource;
    }

    public int getId(){
        return id;
    }

    public String getType(){
        return type;
    }

    public String getDataSource(){
        return dataSource;
    }

    public static ClickType fromType(String type){
        for (ClickType clickType: ClickType.values()){
            if(clickType.getType().equals(type)){
                return clickType;
            }
        }
        return null;
    }

    public static ClickType fromId(int id){
        for (ClickType clickType: ClickType.values()){
            if(clickType.getId() == id){
                return clickType;
            }
        }
        return null;
    }
}
