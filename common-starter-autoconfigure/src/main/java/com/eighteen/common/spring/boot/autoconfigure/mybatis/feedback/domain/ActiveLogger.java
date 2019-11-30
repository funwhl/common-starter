package com.eighteen.common.spring.boot.autoconfigure.mybatis.feedback.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/10/1
 * Time: 22:06
 * SELECT [imei]
 *       ,[imeimd5]
 *       ,[channel]
 *       ,[versionname]
 *       ,[coid]
 *       ,[ncoid]
 *       ,[wifimac]
 *       ,[wifimacmd5]
 *       ,[ip]
 *       ,[activetime]
 *       ,[type]
 *       ,[ua]
 *       ,[androidId]
 *       ,[status]
 *   FROM [srv_15].[KuaiShouFeedback].[dbo].[ActiveLogger]
 */
@Getter
@Setter
public class ActiveLogger {
    private String imei;
    private String imeimd5;
    private String channel;
    private String versionname;
    private String coid;
    private String ncoid;
    private String wifimac;
    private String wifimacmd5;
    private String ip;
    private Date activetime;
    private String type;
    private String ua;
    private String androidId;
    private Integer status;

}
