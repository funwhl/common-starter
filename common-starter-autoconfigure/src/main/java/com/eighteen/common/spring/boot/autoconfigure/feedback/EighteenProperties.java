package com.eighteen.common.spring.boot.autoconfigure.feedback;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by eighteen.
 * Date: 2019/8/18
 * Time: 10:45
 */
@Data
@ConfigurationProperties(prefix = EighteenProperties.PREFIX)
public class EighteenProperties {
    public static final String PREFIX = "18.feedback";
    // 渠道
    private String channel;
    // 同步x分钟内激活数据
    private Integer syncActiveLastMinutes = 10;
    // x天内激活数据保存到历史表
    private Integer channelActive2history = 2;
    // x天内点击数据保存历史表
    private Integer channelclick2history = 2;
    // 点击数据过期周期 天
    private Integer clickDataExpire = 7;
    // 清理imei表 x天内
    private Integer cleanIMEIS = 1;

    //接口回调每次预处理数
    private Integer preFetch = 1000;

    //回调callback字段名
    private String callbackField = "callback";
    //job
    private String cleanClickCron = "0 0 1 1/1 * ?";
    private String cleanImeiCron = "0 0 1 1/1 * ?";
    private String feedbackCron ="0 5/5 * * * ?";
    private String syncActiveCron = "0 1/5 * * * ?";
    private String cleanActiveCron = "0 0 1 1/1 * ?";
    private String dayStatCron = "0 0 1 1/1 * ?";
    private String retentionCron ="0 1/10 * * * ?";

    private Integer mode;
}
