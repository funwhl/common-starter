package com.eighteen.common.feedback.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Table(name = "`active_feedback_match`")
@Data
@Accessors(chain = true)
public class ActiveFeedbackMatch implements Serializable {
    @Id
    @Column(name = "`id`")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`channel`")
    private String channel;

    @Column(name = "`coid`")
    private Integer coid;

    @Column(name = "`ncoid`")
    private Integer ncoid;

    @Column(name = "`firstLinkTime`")
    private Date firstlinktime;

    @Column(name = "`verName`")
    private String vername;

    @Column(name = "`vercode`")
    private Integer vercode;

    @Column(name = "`imei`")
    private String imei;

    @Column(name = "`iimei`")
    private String iimei;

    @Column(name = "`oaid`")
    private String oaid;

    @Column(name = "`androidId`")
    private String androidid;

    @Column(name = "`macAddress`")
    private String macaddress;

    @Column(name = "`manufacture`")
    private String manufacture;

    @Column(name = "`deviceModel`")
    private String devicemodel;

    @Column(name = "`versionRelease`")
    private String versionrelease;

    @Column(name = "`sdk_ver`")
    private String sdkVer;

    @Column(name = "`loc`")
    private Integer loc;

    @Column(name = "`imsi`")
    private String imsi;

    @Column(name = "`wifi`")
    private Integer wifi;

    @Column(name = "`lac`")
    private String lac;

    @Column(name = "`cellID`")
    private String cellid;

    @Column(name = "`resolution`")
    private String resolution;

    @Column(name = "`density`")
    private String density;

    @Column(name = "`ua`")
    private String ua;

    @Column(name = "`utdid`")
    private String utdid;

    @Column(name = "`activeType`")
    private Integer activetype;

    @Column(name = "`activeFrom`")
    private String activefrom;

    @Column(name = "`clientTime`")
    private String clienttime;

    @Column(name = "`isnew`")
    private Integer isnew;

    @Column(name = "`mid`")
    private String mid;

    @Column(name = "`ip`")
    private String ip;

    @JSONField(format="yyyy-MM-ddTHH:mm:ss.SSS")
    @Column(name = "`createTime`")
    private Date createtime;

    @Transient
    private String uuid;

    @Column(name="`type`")
    private String type;

    private static final long serialVersionUID = 1L;

}