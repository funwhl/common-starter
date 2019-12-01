package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:34
 */
@Entity
@Table(name = "t_active_Logger", catalog = "fbdb")
@Accessors(chain = true)
@Data
public class ActiveLogger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    private Integer status;
//    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "imei")
    @Transient
    private ClickLog clickLog;
}
