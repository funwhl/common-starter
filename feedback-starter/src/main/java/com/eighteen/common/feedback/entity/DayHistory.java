package com.eighteen.common.feedback.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

/**
 * Created by wangwei.
 * Date: 2019/12/1
 * Time: 17:24
 */

@Entity
@Table(name = "t_day_history", schema = "dbo", catalog = "Kuaishoufeedback", indexes = {
        @Index(name = "coid", columnList = "coid"),
        @Index(name = "ncoid", columnList = "coid"),
        @Index(name = "value", columnList = "value")
        , @Index(name = "createTime", columnList = "createTime")
})
@Accessors(chain = true)
@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DayHistory)) return false;
        DayHistory history = (DayHistory) o;
        return Objects.equals(getWd(), history.getWd()) &&
                Objects.equals(getValue(), history.getValue()) &&
                Objects.equals(getCoid(), history.getCoid()) &&
                Objects.equals(getNcoid(), history.getNcoid());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getWd(), getValue(), getCoid(), getNcoid());
    }
}
