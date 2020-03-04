package com.eighteen.common.feedback.controller;

import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.FeedBackMapper;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.dao2.ClickLogDao;
import com.eighteen.common.feedback.handler.ClickLogHandler;
import com.eighteen.common.feedback.handler.RetHandler;
import com.eighteen.common.feedback.service.FeedbackService;
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
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    MessageSender sender;
    @Autowired(required = false)
    RetHandler retHandler;
    @Autowired
    FeedbackService feedbackService;
    @Value("${18.feedback.channel}")
    private String channel;
    @Value("${18.feedback.clickQueue:}")
    private String clickQueue;
    @Value("${18.feedback.clickQueue2:Agg.clickReport.Messages.clickLogMessages}")
    private String clickQueue2;

    @GetMapping(value = "clickMonitor")
    public ResponseEntity clickMonitor(@RequestParam Map<String, Object> params, ClickLog clickLog) {
        try {
            logger.debug("click monitor active->{}", params.toString());
            Date date = new Date();
            if (clickLog.getClickTime() == null)
                clickLog.setClickTime(clickLog.getTs() == null ? new Date(System.currentTimeMillis()) : new Date(clickLog.getTs()));
            clickLog.setCreateTime(date);
            if (clickLogHandler != null)
                clickLogHandler.handler(params, clickLog);
            sender.send(channel, clickLog);
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
            return ResponseEntity.ok().body(ImmutableMap.of("ret", 0, "errmsg", "ok"));
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
            value = @Queue(value = "${18.feedback.clickQueue2:Agg.clickReport.Messages.clickLogMessages}"
//                    ,arguments = {@Argument(name = "x-dead-letter-exchange",value = "feedback-dead-exchange"),
//                    @Argument(name = "x-dead-letter-routing-key",value = "${18.feedback.channel}")
//                    ,@Argument(name = "x-message-ttl",value = "2000")
//            }
            ),
            key = "${18.feedback.channel}",
            exchange = @Exchange("${spring.rabbitmq.default-exchange}")
    ))
    public void insertClickLog(@Payload com.eighteen.common.mq.rabbitmq.Message msg) throws Exception {
        RetryTemplate template = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(TimeUnit.MINUTES.toMillis(3));
        template.setBackOffPolicy(fixedBackOffPolicy);
        try {
            template.execute((RetryCallback<Object, Exception>) context -> {
                ClickLog clickLog = (ClickLog) msg.getPayload();
                clickLogDao.save(clickLog);
                return 0;
            });
            ClickLog clickLog = (ClickLog) msg.getPayload();
            clickLogDao.save(clickLog);
        } catch (Exception e) {
            logger.info("save_clickLog_error,{},{}", msg.getPayload().toString(), e.getMessage());
            throw e;
        }
    }

    @GetMapping(value = "clear")
    public void clearDayCache() {
        feedbackService.clearCache(null);
    }

    @GetMapping(value = "sync")
    public void syncDayCache() {
        feedbackService.clearCache(null);

        feedbackService.syncCache();
    }
}
