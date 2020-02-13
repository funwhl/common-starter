package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:34
 */
@Entity
@Table(name = "t_active_Logger",
//        schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei", columnList = "imei"),
                @Index(name = "imeiMd5", columnList = "imeiMd5"),
                @Index(name = "coid", columnList = "coid"),
                @Index(name = "ncoid", columnList = "coid"),
                @Index(name = "oaidMd5", columnList = "oaidMd5"),
                @Index(name = "activeTime", columnList = "activeTime"),
                @Index(name = "androidIdMd5", columnList = "androidIdMd5"),
                @Index(name = "type", columnList = "type")
        })
@Accessors(chain = true)
@Data
public class ActiveLogger {
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
    @Column(name = "channel", columnDefinition = "varchar(15)")
    private String channel;
    private String versionName;
    private Integer coid;
    private Integer ncoid;
    private String wifimac;
    private String wifimacMd5;
    @Column(name = "ip", columnDefinition = "varchar(20)")
    private String ip;
    @Column(name = "type", columnDefinition = "varchar(20)")
    private String type;
    private String ua;
    private Integer status;
    private Date activeTime;
    private Date createTime;
    @Column(name = "mid", columnDefinition = "varchar(100)")
    private String mid;
    @Transient
    private ClickLog clickLog;
}
