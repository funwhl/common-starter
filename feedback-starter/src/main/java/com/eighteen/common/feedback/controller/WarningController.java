package com.eighteen.common.feedback.controller;


import com.eighteen.common.feedback.dao.StatMapper;
import com.eighteen.common.feedback.domain.HourStat;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : wangwei
 * @date : 2019/10/28 9:52
 */
@RestController
@RequestMapping("warning")
public class WarningController {
    @Autowired
    StatMapper statMapper;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Value("${warning.hour:1}")
    private Integer hour;
    @Value("${warning.minute:30}")
    private Integer minute;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @GetMapping
    public Map warning() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -hour);

        List<HourStat> data = statMapper.statByHour(format.format(calendar.getTime()), calendar.get(Calendar.HOUR_OF_DAY));
        Map ret = new HashMap();

        if (CollectionUtils.isEmpty(data) || data.size() < 3) ret.put("ret", 500);
        else ret.put("ret", 200);
        ret.put("data", data);
        return ret;
    }

    @GetMapping(value = "warningMiniute")
    public Map warningMiniute() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minute);

        Long count = statMapper.statMiniute(calendar.getTime());
        Map ret = new HashMap();

        if (count <= 0) ret.put("ret", 500);
        ret.put("ret", 200);
        ret.put(minute + "分钟内回传:", count);
        return ret;
    }
}
