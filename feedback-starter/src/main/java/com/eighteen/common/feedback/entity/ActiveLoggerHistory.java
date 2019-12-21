package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/14
 * Time: 20:11
 */
@Entity
@Table(name = "t_active_Logger_History", schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei", columnList = "imei"),
                @Index(name = "coid", columnList = "coid"),
                @Index(name = "ncoid", columnList = "coid"),
                @Index(name = "oaid", columnList = "oaid")
                , @Index(name = "activeTime", columnList = "activeTime"),
                @Index(name = "androidId", columnList = "androidId")
        })
@Accessors(chain = true)
@Data
public class ActiveLoggerHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition = "varchar(50)")
    private String imei;
    private String imeiMd5;
    private String channel;
    private String versionName;
    private Integer coid;
    private Integer ncoid;
    private String wifimac;
    private String wifimacMd5;
    private String ip;
    private String type;
    private String ua;
    private String androidId;
    private String oaid;
    private Integer status;
    private Date activeTime;
    private Date createTime;
    private String mid;
}
