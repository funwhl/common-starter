package com.eighteen.common.spring.boot.autoconfigure.feedback.controller;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.spring.boot.autoconfigure.feedback.dao.FeedBackMapper;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by eighteen.
 * Date: 2019/8/25
 * Time: 20:09
 */

@RestController
public class ClickMonitorController {
    private static final Logger logger = LoggerFactory.getLogger(ClickMonitorController.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;

    @Value("${18.feedback.channel}")
    private String channel;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping(value = "clickMonitor")
    public void clickMonitor(@RequestParam Map<String, Object> params) {
        try {
            logger.info("click monitor active->{}", params.toString());
            params.put("create_time", new Date());
            if (NumberUtils.isCreatable(String.valueOf(params.get("ts"))))
                params.put("click_time", new Date(Long.valueOf(String.valueOf(params.get("ts")))));
            feedBackMapper.insertClickLog(params);
            rabbitTemplate.convertAndSend("Agg.KuaiShouClickReport.Messages.KuaiShouClickReportMessages",JSONObject.toJSONString(params));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @GetMapping(value = "compensate")
//    public void compensate(@RequestParam("start") String start) {
//        try {
//            logger.info("compensate");
//            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(start);
//            Set<String> imeis = feedBackMapper.getDayImeis(date);
//            List<Map<String, Object>> data = feedBackMapper.getThirdActiveLogger(date, channel);
//
//            data = data.stream().sorted((o1, o2) -> ((Date) o2.get("activetime")).compareTo((Date) o1.get("activetime")))
//                    .collect(
//                            Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> String.valueOf(o2.get("imei"))))), ArrayList::new)
//                    );
//            ListIterator<Map<String, Object>> it = data.listIterator();
//            while (it.hasNext()) {
//                Map<String, Object> map = it.next();
//                String imei = String.valueOf(map.get("imei"));
//                if (imeis.contains(imei)) {
//                    it.remove();
//                    continue;
//                }
//                map.put("imeimd5", DigestUtils.md5DigestAsHex(imei.getBytes()));
//                map.put("wifimacmd5", DigestUtils.md5DigestAsHex(String.valueOf(map.get("wifimac")).getBytes()));
//                feedBackMapper.insertActiveLogger(map);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
