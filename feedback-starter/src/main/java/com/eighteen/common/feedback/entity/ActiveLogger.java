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
@Table(name = "t_active_Logger", schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei",  columnList="imei", unique = true),
                @Index(name = "coid", columnList="coid"),
                @Index(name = "ncoid", columnList="coid"),
                @Index(name = "oaid", columnList="oaid")
//                ,@Index(name = "activeTime", columnList="active_time"),
//                @Index(name = "androidId", columnList="android_id")
})
@Accessors(chain = true)
@Data
public class ActiveLogger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition="varchar(50) COMMENT 'imei'")
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
//    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "imei")
    @Transient
    private ClickLog clickLog;
}
