package com.eighteen.common.feedback;

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
    public Integer syncOffset = 1;
    public Integer cleanActiveOffset = 60;
    public boolean clearCache = false;
    // 渠道
    private String channel;
    private String types;
    private String filters;
    // 点击数据过期周期 天
    private Integer clickDataExpire=1;
    private Integer activeDataExpire =1;
    //接口回调每次预处理数
    private Integer preFetch = 1000;
    private Integer preFetchActive = 5000;
    //job
    private String cleanClickCron = "0 10 04 * * ?";
    private String cleanImeiCron = "0 0 0/1 * * ?";
    private String feedbackCron = "0 1/1 * * * ?";
    private String feedbackColdCron;
    private String syncActiveCron = "0 0/1 * * * ?";
    private String cleanActiveCron = "0 25 04 * * ?";
    private String cleanActiveHistoryCron = "0 7/10 * * * ?";
    private String dayStatCron = "0 0 1 1/1 * ?";
    private String retentionCron = "0 0 0/1 * * ?";

    private Integer offset = 1;
    private Integer sc = 1;
    private String scParam = "";

    private Integer mode;
    private Boolean retention = true;
    private Boolean enable = true;
    private Boolean ipAttributed  = false;
    private Boolean macAttributed  = false;
    private Boolean datetimeAttributed  = false;
    private Boolean allAttributed  = false;
    private Boolean prodAttributed  = false;
    private Boolean channelAttributed  = true;
    private Boolean allActive  = false;
//    private Boolean channelAttributed  = true;
    private Integer matchMinuteOffset = 0;
    private Integer activeMinuteOffset = 60;
    private Integer clickMinuteOffset = 120;
    private Integer coldHourOffset = 2;
    private String coldChannels = "";
    private Boolean multipleImei = false;
    private Boolean doindb = true;
    private Boolean coldData = false;

    private String ignoreImei;
    private Boolean warning = false;
    private Boolean persistClick = false;
    private Boolean persistActive = false;
    private Boolean parale = true;
    private Boolean persistRedisActive = false;
    private Boolean persistRedisClick = false;

    private String appid;
    private String appSecret;

    private Integer prefetchClickHour;
}
