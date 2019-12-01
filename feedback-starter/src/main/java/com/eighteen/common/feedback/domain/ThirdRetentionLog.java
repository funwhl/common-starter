package com.eighteen.common.feedback.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.io.Serializable;

/**
 * @description: ThirdRetentionLog
 * @author: hcl
 * @date: 2019/09/18
 * @version: 1.0.0
 */
@Getter
@Setter
public class ThirdRetentionLog implements Serializable{

    
    private static final long serialVersionUID = 1L;
   
     
    /** channel **/
    private Integer  channel;
     
    /** imei **/
    private String  imei;
     
    /** packname **/
    private String  packname;
     
    /** versionname **/
    private String  versionname;
     
    /** versioncode **/
    private Integer  versioncode;
     
    /** wifi **/
    private Integer  wifi;
     
    /** imsi **/
    private String  imsi;
     
    /** brand **/
    private String  brand;
     
    /** model **/
    private String  model;
     
    /** versionsdk **/
    private String  versionsdk;
     
    /** versionrelease **/
    private String  versionrelease;
     
    /** coid **/
    private Integer  coid;
     
    /** wifimac **/
    private String  wifimac;
     
    /** ip **/
    private String  ip;
     
    /** activetime **/
    private Date  activetime;
     
    /** agent **/
    private String  agent;
     
    /** ncoid **/
    private Integer  ncoid;
     
    /** type **/
    private String  type;
     
    /** imeimd5 **/
    private String  imeimd5;


    // 次留 字段
    private String callBack;

    private String aid;

    private String cid;

    private String mac;

    private String ts;

    private String androidId;

}
