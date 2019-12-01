package com.eighteen.common.feedback.entity;

import lombok.Data;

import java.util.Date;
import java.util.Objects;

/**
 * @author : wangwei
 * @date : 2019/9/18 15:44
 */
@Data
public class DayImei {
    String imei;
    String imeimd5;
    Integer coid;
    Integer ncoid;
    Date createTime;

    public DayImei() {
    }

    public DayImei(String imei, Integer coid, Integer ncoid) {
        this.imei = imei;
        this.coid = coid;
        this.ncoid = ncoid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayImei dayImei = (DayImei) o;
        return Objects.equals(imei, dayImei.imei) &&
                Objects.equals(coid, dayImei.coid) &&
                Objects.equals(ncoid, dayImei.ncoid);
    }

    @Override
    public int hashCode() {

        return Objects.hash(imei, coid, ncoid);
    }
}
