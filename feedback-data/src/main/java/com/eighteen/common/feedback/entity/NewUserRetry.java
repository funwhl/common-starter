package com.eighteen.common.feedback.entity;

import com.eighteen.common.spring.boot.autoconfigure.ds.dynamic.HasDynamicDataSource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Table(name = "`t_new_user_retry`")
public class NewUserRetry implements Serializable, HasDynamicDataSource {
    @Id
    @Column(name = "`id`")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`channel`")
    private String channel;

    @Column(name = "`coid`")
    private Integer coid;

    @Column(name = "`ncoid`")
    private Integer ncoid;

    @Column(name = "`firstLinkTime`")
    private Date firstlinktime;

    @Column(name = "`verName`")
    private String vername;

    @Column(name = "`vercode`")
    private Integer vercode;

    @Column(name = "`imei`")
    private String imei;

    @Column(name = "`iimei`")
    private String iimei;

    @Column(name = "`oaid`")
    private String oaid;

    @Column(name = "`androidId`")
    private String androidid;

    @Column(name = "`macAddress`")
    private String macaddress;

    @Column(name = "`manufacture`")
    private String manufacture;

    @Column(name = "`deviceModel`")
    private String devicemodel;

    @Column(name = "`versionRelease`")
    private String versionrelease;

    @Column(name = "`sdk_ver`")
    private String sdkVer;

    @Column(name = "`loc`")
    private Integer loc;

    @Column(name = "`imsi`")
    private String imsi;

    @Column(name = "`wifi`")
    private Integer wifi;

    @Column(name = "`lac`")
    private String lac;

    @Column(name = "`cellID`")
    private String cellid;

    @Column(name = "`resolution`")
    private String resolution;

    @Column(name = "`density`")
    private String density;

    @Column(name = "`ua`")
    private String ua;

    @Column(name = "`utdid`")
    private String utdid;

    @Column(name = "`activeType`")
    private Integer activetype;

    @Column(name = "`activeFrom`")
    private String activefrom;

    @Column(name = "`clientTime`")
    private String clienttime;

    @Column(name = "`isnew`")
    private Integer isnew;

    @Column(name = "`mid`")
    private String mid;

    @Column(name = "`ip`")
    private String ip;

    @Column(name = "`createTime`")
    private Date createtime;

    private static final long serialVersionUID = 1L;

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel
     */
    public void setChannel(String channel) {
        this.channel = channel == null ? null : channel.trim();
    }

    /**
     * @return coid
     */
    public Integer getCoid() {
        return coid;
    }

    /**
     * @param coid
     */
    public void setCoid(Integer coid) {
        this.coid = coid;
    }

    /**
     * @return ncoid
     */
    public Integer getNcoid() {
        return ncoid;
    }

    /**
     * @param ncoid
     */
    public void setNcoid(Integer ncoid) {
        this.ncoid = ncoid;
    }

    /**
     * @return firstLinkTime
     */
    public Date getFirstlinktime() {
        return firstlinktime;
    }

    /**
     * @param firstlinktime
     */
    public void setFirstlinktime(Date firstlinktime) {
        this.firstlinktime = firstlinktime;
    }

    /**
     * @return verName
     */
    public String getVername() {
        return vername;
    }

    /**
     * @param vername
     */
    public void setVername(String vername) {
        this.vername = vername == null ? null : vername.trim();
    }

    /**
     * @return vercode
     */
    public Integer getVercode() {
        return vercode;
    }

    /**
     * @param vercode
     */
    public void setVercode(Integer vercode) {
        this.vercode = vercode;
    }

    /**
     * @return imei
     */
    public String getImei() {
        return imei;
    }

    /**
     * @param imei
     */
    public void setImei(String imei) {
        this.imei = imei == null ? null : imei.trim();
    }

    /**
     * @return iimei
     */
    public String getIimei() {
        return iimei;
    }

    /**
     * @param iimei
     */
    public void setIimei(String iimei) {
        this.iimei = iimei == null ? null : iimei.trim();
    }

    /**
     * @return oaid
     */
    public String getOaid() {
        return oaid;
    }

    /**
     * @param oaid
     */
    public void setOaid(String oaid) {
        this.oaid = oaid == null ? null : oaid.trim();
    }

    /**
     * @return androidId
     */
    public String getAndroidid() {
        return androidid;
    }

    /**
     * @param androidid
     */
    public void setAndroidid(String androidid) {
        this.androidid = androidid == null ? null : androidid.trim();
    }

    /**
     * @return macAddress
     */
    public String getMacaddress() {
        return macaddress;
    }

    /**
     * @param macaddress
     */
    public void setMacaddress(String macaddress) {
        this.macaddress = macaddress == null ? null : macaddress.trim();
    }

    /**
     * @return manufacture
     */
    public String getManufacture() {
        return manufacture;
    }

    /**
     * @param manufacture
     */
    public void setManufacture(String manufacture) {
        this.manufacture = manufacture == null ? null : manufacture.trim();
    }

    /**
     * @return deviceModel
     */
    public String getDevicemodel() {
        return devicemodel;
    }

    /**
     * @param devicemodel
     */
    public void setDevicemodel(String devicemodel) {
        this.devicemodel = devicemodel == null ? null : devicemodel.trim();
    }

    /**
     * @return versionRelease
     */
    public String getVersionrelease() {
        return versionrelease;
    }

    /**
     * @param versionrelease
     */
    public void setVersionrelease(String versionrelease) {
        this.versionrelease = versionrelease == null ? null : versionrelease.trim();
    }

    /**
     * @return sdk_ver
     */
    public String getSdkVer() {
        return sdkVer;
    }

    /**
     * @param sdkVer
     */
    public void setSdkVer(String sdkVer) {
        this.sdkVer = sdkVer == null ? null : sdkVer.trim();
    }

    /**
     * @return loc
     */
    public Integer getLoc() {
        return loc;
    }

    /**
     * @param loc
     */
    public void setLoc(Integer loc) {
        this.loc = loc;
    }

    /**
     * @return imsi
     */
    public String getImsi() {
        return imsi;
    }

    /**
     * @param imsi
     */
    public void setImsi(String imsi) {
        this.imsi = imsi == null ? null : imsi.trim();
    }

    /**
     * @return wifi
     */
    public Integer getWifi() {
        return wifi;
    }

    /**
     * @param wifi
     */
    public void setWifi(Integer wifi) {
        this.wifi = wifi;
    }

    /**
     * @return lac
     */
    public String getLac() {
        return lac;
    }

    /**
     * @param lac
     */
    public void setLac(String lac) {
        this.lac = lac == null ? null : lac.trim();
    }

    /**
     * @return cellID
     */
    public String getCellid() {
        return cellid;
    }

    /**
     * @param cellid
     */
    public void setCellid(String cellid) {
        this.cellid = cellid == null ? null : cellid.trim();
    }

    /**
     * @return resolution
     */
    public String getResolution() {
        return resolution;
    }

    /**
     * @param resolution
     */
    public void setResolution(String resolution) {
        this.resolution = resolution == null ? null : resolution.trim();
    }

    /**
     * @return density
     */
    public String getDensity() {
        return density;
    }

    /**
     * @param density
     */
    public void setDensity(String density) {
        this.density = density == null ? null : density.trim();
    }

    /**
     * @return ua
     */
    public String getUa() {
        return ua;
    }

    /**
     * @param ua
     */
    public void setUa(String ua) {
        this.ua = ua == null ? null : ua.trim();
    }

    /**
     * @return utdid
     */
    public String getUtdid() {
        return utdid;
    }

    /**
     * @param utdid
     */
    public void setUtdid(String utdid) {
        this.utdid = utdid == null ? null : utdid.trim();
    }

    /**
     * @return activeType
     */
    public Integer getActivetype() {
        return activetype;
    }

    /**
     * @param activetype
     */
    public void setActivetype(Integer activetype) {
        this.activetype = activetype;
    }

    /**
     * @return activeFrom
     */
    public String getActivefrom() {
        return activefrom;
    }

    /**
     * @param activefrom
     */
    public void setActivefrom(String activefrom) {
        this.activefrom = activefrom == null ? null : activefrom.trim();
    }

    /**
     * @return clientTime
     */
    public String getClienttime() {
        return clienttime;
    }

    /**
     * @param clienttime
     */
    public void setClienttime(String clienttime) {
        this.clienttime = clienttime == null ? null : clienttime.trim();
    }

    /**
     * @return isnew
     */
    public Integer getIsnew() {
        return isnew;
    }

    /**
     * @param isnew
     */
    public void setIsnew(Integer isnew) {
        this.isnew = isnew;
    }

    /**
     * @return mid
     */
    public String getMid() {
        return mid;
    }

    /**
     * @param mid
     */
    public void setMid(String mid) {
        this.mid = mid == null ? null : mid.trim();
    }

    /**
     * @return ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * @param ip
     */
    public void setIp(String ip) {
        this.ip = ip == null ? null : ip.trim();
    }

    /**
     * @return createTime
     */
    public Date getCreatetime() {
        return createtime;
    }

    /**
     * @param createtime
     */
    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    @Override
    public String getDataSource() {
        return null;
    }
}