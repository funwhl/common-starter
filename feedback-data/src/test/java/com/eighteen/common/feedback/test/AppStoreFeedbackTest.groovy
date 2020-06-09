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

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

/**
 *
 * @author : wangwei
 * @date : 2020/6/6 15:51
 */
@SpringBootTest(classes = TestRedisManagerConfiguration.class)
class AppStoreFeedbackTest extends Specification {
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

    def "应用商店直投-激活匹配商店点击"() {

        given: '接收点击'
        def storeClick = generateClick("1")
        when(channelConfigService.getByChannel(any(String))).thenReturn(getClickChannelConfig(storeClick))
        def r1 = redisManager.matchNewUserRetry(storeClick, "gdtDir")
        redisManager.saveClickLog(storeClick, "gdtDir")

        and: '接收平台激活'
        def active = generateActive(storeClick, "3")
        def r2 = redisManager.matchClickLog(active)

        and: '接收商店激活'
        active = generateActive(storeClick, "4")
        def r3 = redisManager.matchClickLog(active)

        and: '商店打开失败-兜底激活(同点击渠道)'
        active = generateActive(storeClick, storeClick.getChannel())
        def r4 = redisManager.matchClickLog(active)

        expect: "r1-点击匹配失败,r2-平台匹配失败,r3-商店匹配成功,r4-兜底同渠道匹配成功"
        r1 == null
        r2 == null
        r3 != null
        r4 != null
    }

    def "应用商店直投-激活匹配平台点击"() {

        given: '接收点击'
        def platClick = generateClick("2")
        when(channelConfigService.getByChannel(any(String))).thenReturn(getClickChannelConfig(platClick))
        def r1 = redisManager.matchNewUserRetry(platClick, "gdtDir")
        redisManager.saveClickLog(platClick, "gdtDir")

        and: '接收平台激活'
        def active = generateActive(platClick, "2")
        def r2 = redisManager.matchClickLog(active)

        and: '接收平台激活'
        active = generateActive(platClick, "3")
        def r3 = redisManager.matchClickLog(active)

        and: '接收商店激活'
        active = generateActive(platClick, "4")
        def r4 = redisManager.matchClickLog(active)

        expect: "r1-点击匹配失败,r2-平台同渠道匹配成功,r3-平台不同渠道匹配失败,r4-商店匹配失败"
        r1 == null
        r2 != null
        r3 == null
        r4 == null
    }


    def "应用商店直投-点击匹配retry"() {

        given: '生成点击'
        def click = generateClick("1")
        when(channelConfigService.getByChannel(click.getChannel())).thenReturn(getClickChannelConfig(click))

        and: '点击匹配平台激活'
        def retry = generateRetry(generateActive(click, "3"))
        redisManager.saveNewUserRetry(retry)
        def r1 = redisManager.matchNewUserRetry(click, "gdtDir")

        and: '点击匹配商店激活'
        def retry2 = generateRetry(generateActive(click, "4"))
        redisManager.saveNewUserRetry(retry2)
        def r2 = redisManager.matchNewUserRetry(click, "gdtDir")

        expect: "r1-平台匹配失败,r2-商店匹配成功"
        r1 == null
        r2 != null
    }

    private static ThrowChannelConfig getClickChannelConfig(ClickLog clickLog) {
        def config = new ThrowChannelConfig()
        config.setChannelType(clickLog.getChannel() == "1" ? 2 : 1)
        config.setCoid(clickLog.getCoid())
        config.setNcoid(clickLog.getNcoid())
        config.setChannel(clickLog.getChannel())
        return config
    }

    private static ClickLog generateClick(String channel) {
        return new ClickLog().setId(1).setImei("00000000").setImeiMd5(DigestUtils.getMd5Str("00000000")).setChannel(channel).setCoid(1).setNcoid(1)
    }

    private static ActiveFeedbackMatch generateActive(ClickLog clickLog, String channel) {
        ActiveFeedbackMatch active = new ActiveFeedbackMatch().setId(1).setImei(clickLog.getImei()).setChannel(channel).setCoid(clickLog.getCoid()).setNcoid(clickLog.getNcoid())
        if (channel == "1") active.setType("store")
        if (channel == "2") active.setType("gdt")
        if (channel == "3") active.setType("gdt")
        if (channel == "4") active.setType("store")
        return active
    }

    private static NewUserRetry generateRetry(ActiveFeedbackMatch active) {
        def retry = new NewUserRetry()
        BeanUtils.copyProperties(active, retry)
        retry.setDataSource(active.getType())
        return retry
    }
}
