package com.eighteen.common.feedback.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * @author : wangwei
 * @date : 2019/10/28 10:08
 */
@Setter
@Getter
public class HourStat {
    private String date;
    private Integer hour;
    private String type;
    private Integer count;

    @Override
    public String toString() {
        return "日期:"+date +"," +"小时:" + hour + "," + "指标:"+type +"," +"总计:" + count;
    }
}
