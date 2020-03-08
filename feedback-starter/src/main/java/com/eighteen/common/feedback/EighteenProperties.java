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
    private Integer clickDataExpire;
    //接口回调每次预处理数
    private Integer preFetch = 1000;
    private Integer preFetchActive = 5000;
    //job
    private String cleanClickCron = "0 10/30 * * * ?";
    private String cleanImeiCron = "0 0 0/1 * * ?";
    private String feedbackCron = "0 1/1 * * * ?";
    private String syncActiveCron = "0 0/1 * * * ?";
    private String cleanActiveCron = "0 15/30 * * * ?";
    private String cleanActiveHistoryCron = "0 7/10 * * * ?";
    private String dayStatCron = "0 0 1 1/1 * ?";
    private String retentionCron = "0 0 0/1 * * ?";

    private Integer offset = 1;
    private Integer sc = 1;

    private Integer mode;
    private Boolean retention = true;
    private Boolean enable = true;
    private Boolean ipAttributed  = false;
    private Boolean macAttributed  = false;
    private Boolean datetimeAttributed  = false;
    private Boolean allAttributed  = false;
    private Integer matchMinuteOffset = 0;
    private Integer activeMinuteOffset = 30;
    private Boolean multipleImei = false;
    private Boolean doindb = false;

    private String ignoreImei;
    private Boolean warning = false;
    private Boolean persistClick = true;
    private Boolean persistActive = true;
    private Boolean parale = true;
    private Boolean persistRedis = false;

    private String appid;
    private String appSecret;
}
