package com.eighteen.common.feedback.entity;

import com.eighteen.common.feedback.constants.DsConstants;
import com.eighteen.common.spring.boot.autoconfigure.ds.dynamic.HasDynamicDataSource;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:21
 */

@Entity
@Table(name = "t_click_log",
//        schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei_md5", columnList = "imei_md5"),
                @Index(name = "clickTime", columnList = "clickTime"),
                @Index(name = "android_id_md5", columnList = "android_id_md5"),
                @Index(name = "oaid_md5", columnList = "oaid_md5"),
                @Index(name = "mac", columnList = "mac"),
                @Index(name = "mac2", columnList = "mac2")
        })
@Accessors(chain = true)
@Data
public class ClickLog implements Serializable, HasDynamicDataSource {
    private static final long serialVersionUID = -1L;
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
    @Column(name = "ip", columnDefinition = "varchar(50)")
    private String ip;
    @Column(name = "mac", columnDefinition = "varchar(50)")
    private String mac;
    @Column(name = "mac2", columnDefinition = "varchar(50)")
    private String mac2;
    @Column(name = "callback_url", columnDefinition = "varchar(1500)")
    private String callbackUrl;
    @Column(name = "channel", columnDefinition = "varchar(15)")
    private String channel;
    @Column(name = "mid", columnDefinition = "varchar(100)")
    private String mid;
    @Column(name = "ua", columnDefinition = "varchar(50)")
    private String ua;
    @Column(name = "ipua", columnDefinition = "varchar(50)")
    private String ipua;
    private String param1;
    private String param2;
    private String param3;
    private String param4;
    private Date clickTime;
    private Date createTime;
    @Column(name = "ts", columnDefinition = "bigint")
    private Long ts;
    private Integer status;
    private Integer coid;
    private Integer ncoid;

    /**
     * 渠道类型
     */
    private transient String dataSource;

}
