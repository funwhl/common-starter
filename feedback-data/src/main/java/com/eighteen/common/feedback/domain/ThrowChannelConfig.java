package com.eighteen.common.feedback.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Created by wangwei.
 * Date: 2020/3/17
 * Time: 20:05
 */
@Accessors
public class ThrowChannelConfig {
    private Integer id;
    private Integer coid;
    private Integer ncoid;
    private String channel;
    private String agent;
    private Integer channelType;

    private String platType;
    private String remark;
    private Double rate;
    private Double oriRate;
    private Integer adFilter = 0;
    private Integer feedbackType = 1;
    private Double score = 0d;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCoid() {
        return coid;
    }

    public void setCoid(Integer coid) {
        this.coid = coid;
    }

    public Integer getNcoid() {
        return ncoid;
    }

    public void setNcoid(Integer ncoid) {
        this.ncoid = ncoid;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Integer getChannelType() {
        return channelType;
    }

    public void setChannelType(Integer channelType) {
        this.channelType = channelType;
    }

    public String getPlatType() {
        return platType;
    }

    public void setPlatType(String platType) {
        this.platType = platType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getOriRate() {
        return oriRate;
    }

    public void setOriRate(Double oriRate) {
        this.oriRate = oriRate;
    }

    public Integer getAdFilter() {
        return adFilter;
    }

    public void setAdFilter(Integer adFilter) {
        this.adFilter = adFilter;
    }

    public Integer getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(Integer feedbackType) {
        this.feedbackType = feedbackType;
    }

    public Double getScore() {
        if (this.score==null) return 0d;
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
