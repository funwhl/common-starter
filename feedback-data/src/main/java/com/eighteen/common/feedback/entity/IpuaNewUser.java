package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by wangwei.
 * Date: 2020/2/22
 * Time: 19:17
 */
@Entity
@Table(name = "t_ipua_new_user", indexes = {@Index(name = "ipua", columnList = "ipua")})
@Data
@Accessors(chain = true)
public class IpuaNewUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ipua;
    private String ip;
    private String ua;
    private Integer coid;
    private Integer ncoid;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    @CreationTimestamp
    private Date createTime;
}
