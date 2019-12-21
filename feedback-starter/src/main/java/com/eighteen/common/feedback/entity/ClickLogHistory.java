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
@Table(name = "t_click_log_History", schema = "dbo", catalog = "Kuaishoufeedback")
@Accessors(chain = true)
@Data
public class ClickLogHistory {
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
    private Date clickTime;
    private Date createTime;

}
