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
@Table(name = "t_feedback_Log", catalog = "fbdb")
@Data
@Accessors(chain = true)
public class FeedbackLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imei;
    private String aid;
    private String cid;
    private String mac;
    private String ip;
    private Date ts;
    private Integer channel;
    private Integer eventType;
    private String androidId;
    private String MatchField;
    private String callbackUrl;
    private Date createTime;
}

