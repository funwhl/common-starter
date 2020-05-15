package com.eighteen.common.feedback.test;

import com.eighteen.common.feedback.data.impl.FeedbackRedisManagerImpl;
import com.eighteen.common.feedback.domain.ActiveMatchKeyField;
import com.eighteen.common.feedback.domain.ClickType;
import com.eighteen.common.feedback.domain.MatchClickLogResult;
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

    @Test
    public void testGetActiveMatchKeyFieldsByIImei() {
        var feedbackMatch = new ActiveFeedbackMatch().setIimei("862980030500649,862980030500656").setImei("862980030500649").setAndroidid("21607bf3ba87db4").setOaid("862980030500649");
        List<ActiveMatchKeyField> keys = redisManager.getActiveMatchKeyFields(feedbackMatch);
        keys.forEach(k -> {
            log.info(k.getMatchKey());
        });
        assertThat(keys.stream().map(k -> k.getMatchKey()).collect(Collectors.toList())).contains(DigestUtils.getMd5Str("862980030500656"));
    }

    @Test
    public void testSaveClickLog() {
        var clickLog = new ClickLog().setId(1L).setImeiMd5("62b8440b4a349b8aa46829fd6af1c8f5").setClickType(ClickType.GDT.getType()).setChannel("0");
        redisManager.saveClickLog(clickLog, ClickType.GDT.getType());
        clickLog = redisManager.getClickLog(clickLog.getClickType(), 1L);
        log.info(clickLog.toString());
    }

    @Test
    public void testMatchClickLogWhenGdtZeroChannel() {
        var testImei = "abc";
        var imeiMd5 = DigestUtils.getMd5Str(testImei);
        var clickLog = new ClickLog().setId(1L).setImeiMd5(imeiMd5).setClickType(ClickType.GDT.getType()).setChannel("0");
        redisManager.saveClickLog(clickLog, clickLog.getClickType());
        var feedbackMatch = new ActiveFeedbackMatch().setImei(testImei);
        MatchClickLogResult clickLogResult = redisManager.matchClickLog(feedbackMatch);
        log.info(clickLogResult.toString());
        assertThat(clickLogResult.getClickLogId()).isEqualTo(1L);
    }

}
