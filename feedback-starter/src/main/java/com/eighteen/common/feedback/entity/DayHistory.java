package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:24
 */

@Entity
@Table(name = "t_day_history", catalog = "fbdb")
@Accessors(chain = true)
@Data
public class DayHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String wd;
    private String value;
    private String valueMd5;
    private Integer coid;
    private Integer ncoid;
    private Date createTime;

    @Transient
    private Integer status;

}
