package com.eighteen.common.feedback.controller;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.FeedBackMapper;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.dao2.ClickLogDao;
import com.eighteen.common.feedback.handler.ClickLogHandler;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
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
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Value("${18.feedback.channel}")
    private String channel;
    @Value("${18.feedback.clickQueue:Agg.KuaiShouClickReport.Messages.KuaiShouClickReportMessages}")
    private String clickQueue;
    @Autowired
    ClickLogDao clickLogDao;
    @Autowired(required = false)
    ClickLogHandler clickLogHandler;


    @GetMapping(value = "clickMonitor")
    public void clickMonitor(@RequestParam Map<String, Object> params,  ClickLog clickLog) {
        try {
            logger.info("click monitor active->{}", params.toString());
            if (clickLogHandler != null) {
                clickLogHandler.handler(params);
            } else {
                params.put("create_time", new Date());
                if (NumberUtils.isCreatable(String.valueOf(params.get("ts"))))
//                params.put("click_time", new Date(Long.valueOf(String.valueOf(params.get("ts")))));
                    clickLog.setClickTime(new Date(Long.valueOf(String.valueOf(params.get("ts")))));
//            feedBackMapper.insertClickLog(params);
                clickLog.setAndroidId(params.get("android_Id")==null?"":params.get("android_Id").toString());
                clickLog.setCallbackUrl(params.get("call_back")==null?"":params.get("call_back").toString());
                clickLog.setCreateTime(new Date());
                clickLogDao.save(clickLog);
                String msg = JSONObject.toJSONString(params);
                Message message = MessageBuilder.withBody(msg.getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setContentEncoding("utf-8")
                        .setMessageId(UUID.randomUUID() + "")
                        .build();
                rabbitTemplate.convertAndSend(clickQueue, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping(value = "clickMonitorExact")
    public void clickMonitorExact(@RequestParam Map<String, Object> params) {
        try {
            Map<String,Object> fields = params.entrySet().stream().filter(o -> o.getKey().startsWith("@@"))
                    .collect(Collectors.toMap(o -> o.getKey().substring(2,o.getKey().length()), Map.Entry::getValue));
            logger.info("click monitor active->{}", params.toString());
            fields.put("create_time", new Date());
            if (NumberUtils.isCreatable(String.valueOf(fields.get("ts"))))
                fields.put("click_time", new Date(Long.valueOf(String.valueOf(params.get("ts")))));
            feedBackMapper.insertClickLog(fields);
            String msg = JSONObject.toJSONString(fields);
            Message message = MessageBuilder.withBody(msg.getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("utf-8")
                    .setMessageId(UUID.randomUUID() + "")
                    .build();
            rabbitTemplate.convertAndSend(clickQueue, message);
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
