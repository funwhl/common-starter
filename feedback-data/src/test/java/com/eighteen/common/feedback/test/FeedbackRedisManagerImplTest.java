package com.eighteen.common.feedback.test;

import com.eighteen.common.feedback.constants.DsConstants;
import com.eighteen.common.feedback.data.impl.FeedbackRedisManagerImpl;
import com.eighteen.common.feedback.domain.ActiveMatchKeyField;
import com.eighteen.common.feedback.domain.ClickType;
import com.eighteen.common.feedback.domain.MatchClickLogResult;
import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.service.ChannelConfigService;
import com.eighteen.common.feedback.service.FeedbackConfigService;
import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate;
import com.eighteen.common.utils.DigestUtils;
import io.netty.channel.ChannelConfig;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.DigestInputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestRedisManagerConfiguration.class)
@Slf4j
public class FeedbackRedisManagerImplTest {

    @InjectMocks
    private FeedbackRedisManagerImpl redisManager;

    @Mock
    private ChannelConfigService channelConfigService;

    @Mock
    private FeedbackConfigService feedbackConfigService;

    @Spy
    @Autowired
    PikaTemplate pikaTemplate;

    @Spy
    @Autowired
    RedisTemplate redisTemplate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    //测试双卡能正确获取用于匹配的key
    @Test
    public void getActiveMatchKeyFields_IImei_existsKey() {
        var feedbackMatch = new ActiveFeedbackMatch().setIimei("862980030500649,862980030500656").setImei("862980030500649").setAndroidid("21607bf3ba87db4").setOaid("862980030500649");
        List<ActiveMatchKeyField> keys = redisManager.getActiveMatchKeyFields(feedbackMatch);
        keys.forEach(k -> {
            log.info(k.getMatchKey());
        });
        assertThat(keys.stream().map(k -> k.getMatchKey()).collect(Collectors.toList())).contains(DigestUtils.getMd5Str("862980030500656"));
    }

    //测试redis正常保存
    @Test
    public void saveClickLog_redisWork_success() {
        var clickLog = new ClickLog().setId(1L).setImeiMd5("62b8440b4a349b8aa46829fd6af1c8f5").setClickType(ClickType.GDT.getType()).setChannel("0");
        redisManager.saveClickLog(clickLog, ClickType.GDT.getType());
        clickLog = redisManager.getClickLog(clickLog.getClickType(), 1L);
        assertThat(clickLog).isNotNull();
    }

    //测试广点通正常匹配0渠道（0渠道没有coid、ncoid数据，0默认为全网归因）
    @Test
    public void matchClickLog_gdtZeroChannel_matched() {
        var testImei = "gdtZeroChannelImei";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.GDT);
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(1L);
    }

    //测试非gdt的0渠道不应该匹配成功
    @Test
    public void matchClickLog_notGdtZeroChannel_notMatched() {
        var testImei = "gdtZeroChannelImei";
        var clickLog = getBaseClickLog(testImei, ClickType.TOUTIAO).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("1");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult).isNull();
    }

    //测试广点通多渠道匹配最大的Id
    @Test
    public void matchClickLog_gdtMultiChannel_matchBigId() {
        var testImei = "gdtMultiChannelImei";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(2L).setChannel("1");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        //mock gdt渠道为全网归因
        when(channelConfigService.getByChannel("1")).thenReturn(mockChannelConfig(0));

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.GDT).setChannel("2");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(2L);
    }

    //测试非广点通渠道 不应该匹配gdt0渠道
    @Test
    public void matchClickLog_notGdtSource_notMatchGdt() {
        var testImei = "notGdtSourceImei";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("2");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickType()).isEqualTo(ClickType.TOUTIAO.getType());
    }

    //测试渠道匹配不上时，使用gdt0渠道归因
    @Test
    public void matchClickLog_channelNotMatch_useGdtZeroChannel() {
        var testImei = "channelNotMatchImei";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("3");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickType()).isEqualTo(ClickType.GDT.getType());
    }

    //测试渠道无法匹配时使用全网归因渠道
    @Test
    public void matchClickLog_channelNotMatchWithMatchAllChannel_useMatchAllChannel() {
        var testImei = "channelNotMatchWithMatchAllChannel";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(10L).setClickType(ClickType.TOUTIAO.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        //设置渠道3为全网归因
        when(channelConfigService.getByChannel("3")).thenReturn(mockChannelConfig(0));

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("4");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(10L);
    }

    //测试渠道无法匹配时 使用其他数据源的全网归因渠道
    @Test
    public void matchClickLog_channelNotMatchWithDiffrentMatchAllChannel_useMatchAllChannel() {
        var testImei = "channelNotMatchWithDiffrentMatchAllChannel";
        var clickLog = getBaseClickLog(testImei, ClickType.TOUTIAO).setId(1L).setChannel("2");
        clickLog.setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(10L).setClickType(ClickType.WX.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        //设置渠道3为全网归因
        when(channelConfigService.getByChannel("3")).thenReturn(mockChannelConfig(0));

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("4");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(10L);
        assertThat(clickLogResult.getClickType()).isEqualTo(ClickType.WX.getType());
    }

    //测试渠道无法匹配时 使用gdt0渠道归因
    @Test
    public void matchClickLog_channelNotMatchWithGdtZeroChannel_useZeroChannel() {
        var testImei = "channelNotMatchWithGdtZeroChannel";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(10L).setClickType(ClickType.WX.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        //设置渠道3为全网归因
        when(channelConfigService.getByChannel("3")).thenReturn(mockChannelConfig(0));

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.TOUTIAO).setChannel("4");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);

        //gdt 0 渠道默认全网归因
        assertThat(clickLogResult.getClickLogId()).isEqualTo(1L);
        assertThat(clickLogResult.getClickType()).isEqualTo(ClickType.GDT.getType());
    }

    //测试混合数据源时匹配最大的Id
    @Test
    public void matchClickLog_hybridSource_matchBigId() {
        var testImei = "hybridSourceImei";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(2L).setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(3L).setClickType(ClickType.WX.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.WEIXIN).setChannel("3");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(3L);
    }

    //测试多个channel在一个数据源 根据channel匹配点击id
    @Test
    public void matchClickLog_multiChannelInSameSource_matchChannelLogId() {
        var testImei = "multiChannelInSameSource";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(2L).setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(3L).setClickType(ClickType.WX.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(4L).setClickType(ClickType.WX.getType()).setChannel("4");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.WEIXIN).setChannel("3");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(3L);
    }

    //测试多个channel在一个数据源 根据channel和全网归因渠道匹配最大的点击Id
    @Test
    public void matchClickLog_hasMatchAllChannelInSameSource_matchBigId() {
        var testImei = "hasMatchAllChannelInSameSource";
        var clickLog = getBaseClickLog(testImei, ClickType.GDT).setId(1L).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(2L).setClickType(ClickType.TOUTIAO.getType()).setChannel("2");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(3L).setClickType(ClickType.WX.getType()).setChannel("3");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        clickLog.setId(4L).setClickType(ClickType.WX.getType()).setChannel("4");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());

        when(channelConfigService.getByChannel("4")).thenReturn(mockChannelConfig(0));

        var feedbackMatch = getBaseActiveFeedbackMatch(testImei, DsConstants.WEIXIN).setChannel("3");
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        assertThat(clickLogResult.getClickLogId()).isEqualTo(4L);
    }

    private ClickLog getBaseClickLog(String testImei, ClickType clickType) {
        var imeiMd5 = DigestUtils.getMd5Str(testImei);
        var clickLog = new ClickLog().setImeiMd5(imeiMd5).setClickType(clickType.getType()).setCoid(1).setNcoid(1);
        return clickLog;
    }

    private ActiveFeedbackMatch getBaseActiveFeedbackMatch(String testImei, String type) {
        return new ActiveFeedbackMatch().setImei(testImei).setType(type).setCoid(1).setNcoid(1);
    }

    private ThrowChannelConfig mockChannelConfig(int channelType) {
        var config = new ThrowChannelConfig();
        config.setChannelType(channelType);
        config.setCoid(1);
        config.setNcoid(1);
        return config;
    }
}
