package com.eighteen.common.spring.boot.autoconfigure.feedback.controller;

import com.eighteen.common.spring.boot.autoconfigure.feedback.dao.FeedBackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Created by eighteen.
 * Date: 2019/8/25
 * Time: 0:09
 */

@RestController
@Configuration
public class ClickMonitorController implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(ClickMonitorController.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    private  String token;
    @Value("${spring.application.name}")
    private  String appName;
    @Value("${accessKey}")
    private  String accessKey;

    @GetMapping(value = "clickMonitor")
    public void test(@RequestParam Map<String, Object> params) {
        try {
            if (!params.containsKey("token")||!params.get("token").equals(token)) {
                logger.error("token not found-> {}",token);
                return;
            }
            params.remove("token");
            feedBackMapper.insertClickLog(params);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }



    @Override
    public void afterPropertiesSet() {
        String a = Base64.getEncoder().encodeToString(accessKey.getBytes(StandardCharsets.UTF_8));
        String b = Base64.getEncoder().encodeToString(appName.getBytes(StandardCharsets.UTF_8));
        token = Base64.getEncoder().encodeToString((a + b).getBytes(StandardCharsets.UTF_8));
        logger.info("click-monitor token: {}",token);
    }


}
