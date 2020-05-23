package com.eighteen.common.feedback.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.eighteen.common.feedback.constants.Constants;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.utils.DigestUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * @author : wangwei
 * @date : 2020/5/11 17:30
 */
@Accessors(chain = true)
@Data
public class AdverLog implements Serializable {
    public String Imei;
    public String BusiID;
    public Integer Coid;
    public Integer NCoid;
    public String VerName;
    public String Channel;
    public Integer PlaceID;
    public String PositionID;
    public Integer SourceID;
    public Integer Type;
    public String IP;
    @JSONField(format="yyyy-MM-ddTHH:mm:ss")
    public Date CreateTime = new Date();
    public String Brand;
    public Integer NetType;
    public Integer InsertCardSpan;
    public String AdverID;
    public String Title;
    public String Desc;
    public String AppPackage;
    /// <summary>
    /// 回传参数
    /// </summary>
    public String CallbackExtra;
    /// <summary>
    /// 广告SDK版本
    /// </summary>
    public String AdSdkVer;

    public String Tag;

    public Integer Sdk_ver;

    public String VersionRelease;

    public String AndroidId;

    public String Manufacture;

    public String Imsi;

    public String MacAddress;

    public String oaid;

    public String mid;

    public String model;

    @JSONField(format="yyyy-MM-ddTHH:mm:ss")
    public Date firstLinkTime;

    public ActiveFeedbackMatch convert2Active() {
        return new ActiveFeedbackMatch().setAndroidid(this.AndroidId).setOaid(this.oaid).setImei(this.Imei);
    }

    public ClickLog convert2Click() {
        return new ClickLog().setAndroidId(getMd5(this.AndroidId)).setOaid(getMd5(this.oaid)).setImei(getMd5(this.Imei));
    }

    public String getMd5(String value) {
        if (!Constants.excludeKeys.contains(value)) return DigestUtils.getMd5Str(value);
        return null;
    }
}
