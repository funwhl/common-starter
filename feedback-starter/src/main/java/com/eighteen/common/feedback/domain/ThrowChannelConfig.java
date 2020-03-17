package com.eighteen.common.feedback.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created by wangwei.
 * Date: 2020/3/17
 * Time: 20:05
 */
@Data
@Accessors
public class ThrowChannelConfig {
    private Integer id;
    private Integer coid;
    private Integer ncoid;
    private String channel;
    private String agent;
    private Integer channelType;
    private String platType;
    private String remark;
    private Double rate;
}
