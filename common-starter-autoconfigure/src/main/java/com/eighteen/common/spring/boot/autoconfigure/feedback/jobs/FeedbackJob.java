package com.eighteen.common.spring.boot.autoconfigure.feedback.jobs;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.eighteen.common.spring.boot.autoconfigure.feedback.dao.FeedBackMapper;
import com.eighteen.common.spring.boot.autoconfigure.job.TaskJob;
import com.eighteen.common.spring.boot.autoconfigure.web.HttpClientUtils;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//import HttpClientUtils;

/**
 * @author : eighteen
 * @date : 2019/8/23 11:02
 */

@TaskJob(jobName = "FeedbackJob", cron = "${18.feedback.feedback-cron:0 5/5 * * * ? }", desc = "快手回调job")
public class FeedbackJob implements SimpleJob {
    public static final Logger logger = LoggerFactory.getLogger(FeedbackJob.class);
    @Autowired(required = false)
    FeedBackMapper fbMapper;
    @Value("${18.feedback.pre-fetch:1000}")
    private Integer count;
    private ExecutorService executor = new ThreadPoolExecutor(6, 6,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    @Override
    public void execute(ShardingContext shardingContext) {
        logger.info("start FeedbackJob");
        Long start = System.currentTimeMillis();
        List<Map<String, Object>> results = fbMapper.getPreFetchData(count);
        if (results != null) results.forEach(o -> executor.execute(() -> {
            try {
                String imei = o.get("imei") == null ? null : (String) o.get("imei");
                Integer coid = o.get("coid") == null ? null : (Integer) o.get("coid");
                Integer aoid = o.get("aoid") == null ? null : (Integer) o.get("aoid");
                if (fbMapper.countFromStatistics(imei, coid, aoid) > 0) return;
                String url = o.get("callback") + "&event_type=1&event_time=" + System.currentTimeMillis();
                String ret = HttpClientUtils.get(url);
                logger.info("callback->url:{}, return:{}", url, ret);
                if (ret.equals("1")) {
                    Map params = new ImmutableMap.Builder<String, Object>()
                            .put("CreateTime", new Date())
                            .put("imei", o.get("md5"))
                            .put("aid", o.get("aid"))
                            .put("cid", o.get("cid"))
                            .put("mac", o.get("mac"))
                            .put("ip", o.get("ip"))
                            .put("ts", o.get("ts"))
                            .put("EventType", o.get("event_type"))
                            .put("ANDROIDID", o.get("android_id"))
                            .put("callback", o.get("callback"))
                            .put("callback_url", url)
                            .build();

                    fbMapper.insertDayImei(imei,
                            o.get("md5") == null ? null : (String) o.get("imei"), new Date());
                    fbMapper.insertFeedback(params);
                }
            } catch (Exception e) {
                logger.error("feedback error:{}", e.getMessage());
            }
        }));

        logger.info("finish FeedbackJob in : {}ms", System.currentTimeMillis() - start);
    }

}
