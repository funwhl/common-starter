package com.eighteen.common.feedback.controller;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.FeedBackMapper;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.dao2.ClickLogDao;
import com.eighteen.common.feedback.handler.ClickLogHandler;
import com.eighteen.common.feedback.handler.RetHandler;
import com.eighteen.common.mq.rabbitmq.MessageSender;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
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
    @Autowired
    ClickLogDao clickLogDao;
    @Autowired(required = false)
    ClickLogHandler clickLogHandler;
    @Value("${18.feedback.channel}")
    private String channel;
    @Value("${18.feedback.clickQueue:}")
    private String clickQueue;
    @Value("${18.feedback.clickQueue2:Agg.clickReport.Messages.clickLogMessages}")
    private String clickQueue2;
    @Autowired
    MessageSender sender;
    @Autowired(required = false)
    RetHandler retHandler;

    @GetMapping(value = "clickMonitor")
    public ResponseEntity clickMonitor(@RequestParam Map<String, Object> params, ClickLog clickLog) {
        try {
            logger.debug("click monitor active->{}", params.toString());
            Date date = new Date();
            params.put("create_time", date);
            if (NumberUtils.isCreatable(String.valueOf(params.get("ts")))) {
                Long ts = Long.valueOf(String.valueOf(params.get("ts")));
                clickLog.setClickTime(new Date(ts));
                clickLog.setTs(ts);
            } else {
                clickLog.setClickTime(date);
                clickLog.setTs(date.getTime());
            }
            clickLog.setAndroidId(params.get("android_Id") == null ? "" : params.get("android_Id").toString());
            if (clickLog.getCallbackUrl()==null)clickLog.setCallbackUrl(params.get("call_back") == null ? "" : params.get("call_back").toString());
            clickLog.setCreateTime(date);
            if (clickLogHandler!=null)
                clickLogHandler.handler(params, clickLog);
            sender.send(channel,clickLog);
            if (StringUtils.isNotBlank(clickQueue)) {
                String msg = JSONObject.toJSONString(params);
                Message message = MessageBuilder.withBody(msg.getBytes())
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setContentEncoding("utf-8")
                        .setMessageId(UUID.randomUUID() + "")
                        .build();
                rabbitTemplate.convertAndSend(clickQueue, message);
            }
            if (retHandler != null) return ResponseEntity.ok(retHandler.ret());
            return ResponseEntity.ok().body(ImmutableMap.of("ret",0,"errmsg","ok"));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            if (retHandler != null) return ResponseEntity.ok(retHandler.ret());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "clickMonitorExact")
    public void clickMonitorExact(@RequestParam Map<String, Object> params) {
        try {
            Map<String, Object> fields = params.entrySet().stream().filter(o -> o.getKey().startsWith("@@"))
                    .collect(Collectors.toMap(o -> o.getKey().substring(2, o.getKey().length()), Map.Entry::getValue));
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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("${18.feedback.clickQueue2:Agg.clickReport.Messages.clickLogMessages}"),
            key = "${18.feedback.channel}",
            exchange = @Exchange("${spring.rabbitmq.default-exchange}")
    ))
    public void insertClickLog(@Payload com.eighteen.common.mq.rabbitmq.Message msg) {
        ClickLog clickLog = (ClickLog) msg.getPayload();
        clickLogDao.save(clickLog);
    }
}
