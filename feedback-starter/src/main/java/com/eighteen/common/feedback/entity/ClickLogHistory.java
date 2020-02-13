package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:43
 */
@Entity
@Table(name = "t_click_log_History"
//        ,schema = "dbo", catalog = "Kuaishoufeedback"
        ,indexes = {@Index(name = "imeiMd5", columnList = "imeiMd5"),
        @Index(name = "clickTime", columnList = "clickTime"),
        @Index(name = "androidIdMd5", columnList = "androidIdMd5"),
        @Index(name = "oaidMd5", columnList = "oaidMd5"),
        @Index(name = "mac", columnList = "mac"),
        @Index(name = "mac2", columnList = "mac2")}
)
@Accessors(chain = true)
@Data
public class ClickLogHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition = "varchar(50)")
    private String imei;
    @Column(name = "imeiMd5", columnDefinition = "varchar(50)")
    private String imeiMd5;
    @Column(name = "androidId", columnDefinition = "varchar(50)")
    private String androidId;
    @Column(name = "androidIdMd5", columnDefinition = "varchar(50)")
    private String androidIdMd5;
    private String oaid;
    @Column(name = "oaidMd5", columnDefinition = "varchar(50)")
    private String oaidMd5;
    @Column(name = "ip", columnDefinition = "varchar(20)")
    private String ip;
    private String mac;
    private String mac2;
    @Column(name = "callback_url", columnDefinition = "varchar(1500)")
    private String callbackUrl;
    @Column(name = "channel", columnDefinition = "varchar(15)")
    private String channel;
    @Column(name = "mid", columnDefinition = "varchar(100)")
    private String mid;
    private String param1;
    private String param2;
    private String param3;
    private String param4;
    private Date clickTime;
    private Date createTime;
    private Long ts;
}
