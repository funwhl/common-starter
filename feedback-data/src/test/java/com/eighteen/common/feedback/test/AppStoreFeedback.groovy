package com.eighteen.common.feedback.test

import com.eighteen.common.feedback.data.impl.FeedbackRedisManagerImpl
import com.eighteen.common.feedback.domain.ThrowChannelConfig
import com.eighteen.common.feedback.entity.ActiveFeedbackMatch
import com.eighteen.common.feedback.entity.ClickLog
import com.eighteen.common.feedback.entity.NewUserRetry
import com.eighteen.common.feedback.service.ChannelConfigService
import com.eighteen.common.feedback.service.FeedbackConfigService
import com.eighteen.common.spring.boot.autoconfigure.pika.PikaTemplate
import com.eighteen.common.utils.DigestUtils
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import spock.lang.Specification

import static org.mockito.Mockito.when

/**
 *
 * @author : wangwei
 * @date : 2020/6/6 15:51
 */
@SpringBootTest(classes = TestRedisManagerConfiguration.class)
class AppStoreFeedback extends Specification {
    @InjectMocks
    private FeedbackRedisManagerImpl redisManager

    @Mock
    private ChannelConfigService channelConfigService

    @Mock
    private FeedbackConfigService feedbackConfigService

    @Spy
    @Autowired
    PikaTemplate pikaTemplate

    @Spy
    @Autowired
    RedisTemplate redisTemplate

    void setup() {
        MockitoAnnotations.initMocks(this)
    }

    def "应用商店直投-激活匹配点击"() {
        given: '接收点击'
        def c1 = generateClick()
        when(channelConfigService.getByChannel(c1.getChannel())).thenReturn(getClickChannelConfig(2, c1))
        def r1 = redisManager.matchNewUserRetry(c1, "gdtDir")
        redisManager.saveClickLog(c1, "gdtDir")

        and: '接收平台激活'
        def active = new ActiveFeedbackMatch().setType("gdt")
        BeanUtils.copyProperties(c1, active)
        active.setChannel("2")
        def r2 = redisManager.matchClickLog(active)


        and: '接收商店激活'
        active = new ActiveFeedbackMatch().setType("store")
        BeanUtils.copyProperties(c1, active)
        active.setChannel("2")
        def r3 = redisManager.matchClickLog(active)


        expect: "平台匹配失败,商店匹配成功"
        r1 == null
        r2 == null
        r3 != null
    }

    def "应用商店直投-点击匹配retry"() {

        given: '生成点击'
        def c1 = generateClick()
        when(channelConfigService.getByChannel(c1.getChannel())).thenReturn(getClickChannelConfig(2, c1))

        and: '点击匹配平台激活'
        def retry = new NewUserRetry()
        BeanUtils.copyProperties(c1, retry)
        retry.setChannel("3")
        retry.setDataSource("gdt")
        redisManager.saveNewUserRetry(retry)
        def r1 = redisManager.matchNewUserRetry(c1, "gdtDir")

        and: '点击匹配商店激活'
        def retry2 = new NewUserRetry()
        BeanUtils.copyProperties(c1, retry2)
        retry2.setChannel("2")
        retry2.setDataSource("store")
        redisManager.saveNewUserRetry(retry2)
        def r2 = redisManager.matchNewUserRetry(c1, "gdtDir")


        expect: "平台匹配失败,商店匹配成功"
        r1 == null
        r2 != null
    }

    private ThrowChannelConfig getClickChannelConfig(int channelType, ClickLog clickLog) {
        def config = new ThrowChannelConfig()
        config.setChannelType(channelType)
        config.setCoid(clickLog.getCoid())
        config.setNcoid(clickLog.getNcoid())
        config.setChannel(clickLog.getChannel())
        return config
    }

    private ClickLog generateClick() {
        return new ClickLog().setId(1).setImei("00000000").setImeiMd5(DigestUtils.getMd5Str("00000000")).setChannel("1").setCoid(1).setNcoid(1)
    }
}
