package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:40
 */
@Entity
@Table(name = "t_feedback_Log",
//        schema = "dbo", catalog = "Kuaishoufeedback",
        indexes = {@Index(name = "imei",  columnList="imei")
        })
@Data
@Accessors(chain = true)
public class FeedbackLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "imei", columnDefinition = "varchar(50)")
    private String imei;
    private String aid;
    private String cid;
    private String mac;
    private String ip;
    private String oaid;
    private String mid;
    private Date ts;
    private Integer channel;
    private Integer eventType;
    private String androidId;
    private String MatchField;
    @Column(name = "callback_url", columnDefinition = "varchar(1500)")
    private String callbackUrl;
    private Integer coid;
    private Integer ncoid;
    private Date createTime;
}

