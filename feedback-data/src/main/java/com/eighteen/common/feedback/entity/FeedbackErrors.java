package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2020/3/10
 * Time: 20:30
 */
@Entity
@Table(name = "t_feedback_errors")
@Data
@Accessors(chain = true)
public class FeedbackErrors {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "channel", columnDefinition = "varchar(15)")
    private String channel;
    @Column(name = "msg", columnDefinition = "varchar(1500)")
    private String msg;
    private Integer coid;
    private Integer ncoid;
    private String type;

    private Date createTime;
}
