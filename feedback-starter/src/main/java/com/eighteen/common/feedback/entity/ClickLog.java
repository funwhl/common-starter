package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:21
 */

@Entity
@Table(name = "t_click_log",
//        schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei", columnList = "imei"),
                @Index(name = "clickTime", columnList = "clickTime"),
                @Index(name = "androidId", columnList = "androidId"),
                @Index(name = "oaid", columnList = "oaid"),
                @Index(name = "mac", columnList = "mac")
        })
@Accessors(chain = true)
@Data
public class ClickLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition = "varchar(50)")
    private String imei;
    private String aid;
    private String cid;
    private String did;
    private String ip;
    private String androidId;
    private String oaid;
    private String mac;
    private String mac2;
    private Integer channel;
    @Column(name = "callback_url", columnDefinition = "varchar(1500)")
    private String callbackUrl;
    private Date clickTime;
    private Date createTime;
    private Long ts;

}
