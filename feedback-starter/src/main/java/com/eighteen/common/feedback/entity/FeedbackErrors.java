package com.eighteen.common.feedback.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2020/3/10
 * Time: 20:30
 */
public class FeedbackErrors {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition = "varchar(50)")
    private String imei;
    @Column(name = "imei_md5", columnDefinition = "varchar(50)")
    private String imeiMd5;
    @Column(name = "android_id", columnDefinition = "varchar(50)")
    private String androidId;
    @Column(name = "android_id_md5", columnDefinition = "varchar(50)")
    private String androidIdMd5;
    private String oaid;
    @Column(name = "oaid_md5", columnDefinition = "varchar(50)")
    private String oaidMd5;
    private String mac;
    @Column(name = "ip", columnDefinition = "varchar(50)")
    private String ip;
    @Column(name = "mid", columnDefinition = "varchar(100)")
    private String mid;
    @Column(name = "ts", columnDefinition = "bigint")
    private Long ts;
    @Column(name = "channel", columnDefinition = "varchar(15)")
    private String channel;
    @Column(name = "active_channel", columnDefinition = "varchar(15)")
    private String activeChannel;
    private Integer eventType;
    private String MatchField;
    @Column(name = "callback_url", columnDefinition = "varchar(1500)")
    private String callbackUrl;
    private Integer coid;
    private Integer ncoid;
    private Integer plot;
    private String param1;
    private String param2;
    private String param3;
    private String param4;
    private Date createTime;
    private Date activeTime;
    private Date clickTime;
}
