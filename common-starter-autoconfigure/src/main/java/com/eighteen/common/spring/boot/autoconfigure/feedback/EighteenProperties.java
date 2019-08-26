package com.eighteen.common.spring.boot.autoconfigure.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by eighteen.
 * Date: 2019/8/18
 * Time: 10:45
 */

@ConfigurationProperties(prefix = EighteenProperties.PREFIX)
public class EighteenProperties {
    public static final String PREFIX = "18.feedback";
    // 渠道
    private String channel;
    // 同步x分钟内激活数据
    private Integer syncActiveLastMinutes = 10;
    // 渠道激活保存到历史表
    private Integer channelActive2history = 2;
    // 点击数据保存历史表
    private Integer channelclick2history = 2;
    // 点击数据过期周期
    private Integer clickDataExpire = 7;
    // 清理imei 临时表
    private Integer imeisTableExpire = 1;

    //接口回调每次预处理数
    private Integer preFetch = 1000;
    //回调callback字段名
    private String callbackField = "callback";
    //job
    private String clieanClickCron;
    private String cleanImeisCron;
    private String feedbackCron;
    private String syncActiveCron;
    private String transferActiveCron;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getSyncActiveLastMinutes() {
        return syncActiveLastMinutes;
    }

    public void setSyncActiveLastMinutes(Integer syncActiveLastMinutes) {
        this.syncActiveLastMinutes = syncActiveLastMinutes;
    }

    public Integer getChannelActive2history() {
        return channelActive2history;
    }

    public void setChannelActive2history(Integer channelActive2history) {
        this.channelActive2history = channelActive2history;
    }

    public Integer getChannelclick2history() {
        return channelclick2history;
    }

    public void setChannelclick2history(Integer channelclick2history) {
        this.channelclick2history = channelclick2history;
    }

    public Integer getClickDataExpire() {
        return clickDataExpire;
    }

    public void setClickDataExpire(Integer clickDataExpire) {
        this.clickDataExpire = clickDataExpire;
    }

    public Integer getPreFetch() {
        return preFetch;
    }

    public void setPreFetch(Integer preFetch) {
        this.preFetch = preFetch;
    }

    public String getCallbackField() {
        return callbackField;
    }

    public void setCallbackField(String callbackField) {
        this.callbackField = callbackField;
    }

    public Integer getImeisTableExpire() {
        return imeisTableExpire;
    }

    public void setImeisTableExpire(Integer imeisTableExpire) {
        this.imeisTableExpire = imeisTableExpire;
    }

    public String getClieanClickCron() {
        return clieanClickCron;
    }

    public void setClieanClickCron(String clieanClickCron) {
        this.clieanClickCron = clieanClickCron;
    }

    public String getCleanImeisCron() {
        return cleanImeisCron;
    }

    public void setCleanImeisCron(String cleanImeisCron) {
        this.cleanImeisCron = cleanImeisCron;
    }

    public String getFeedbackCron() {
        return feedbackCron;
    }

    public void setFeedbackCron(String feedbackCron) {
        this.feedbackCron = feedbackCron;
    }

    public String getSyncActiveCron() {
        return syncActiveCron;
    }

    public void setSyncActiveCron(String syncActiveCron) {
        this.syncActiveCron = syncActiveCron;
    }

    public String getTransferActiveCron() {
        return transferActiveCron;
    }

    public void setTransferActiveCron(String transferActiveCron) {
        this.transferActiveCron = transferActiveCron;
    }

    @Override
    public String toString() {
        return "EighteenProperties{" +
                "channel='" + channel + '\'' +
                ", syncActiveLastMinutes=" + syncActiveLastMinutes +
                ", channelActive2history=" + channelActive2history +
                ", channelclick2history=" + channelclick2history +
                ", clickDataExpire=" + clickDataExpire +
                ", imeisTableExpire=" + imeisTableExpire +
                ", preFetch=" + preFetch +
                ", callbackField='" + callbackField + '\'' +
                ", clieanClickCron='" + clieanClickCron + '\'' +
                ", cleanImeisCron='" + cleanImeisCron + '\'' +
                ", feedbackCron='" + feedbackCron + '\'' +
                ", syncActiveCron='" + syncActiveCron + '\'' +
                ", transferActiveCron='" + transferActiveCron + '\'' +
                '}';
    }
}
