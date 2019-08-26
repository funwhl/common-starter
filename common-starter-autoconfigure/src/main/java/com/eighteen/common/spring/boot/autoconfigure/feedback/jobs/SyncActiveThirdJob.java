package com.eighteen.common.spring.boot.autoconfigure.feedback.jobs;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.spring.boot.autoconfigure.feedback.dao.FeedBackMapper;
import com.eighteen.common.spring.boot.autoconfigure.job.TaskJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author : eighteen
 * @date : 2019/8/23 10:29
 */

@TaskJob(jobName = "SyncActiveThirdJob", cron = "${18.feedback.sync-active-cron:0 1/5 * * * ? }", desc = "同步激活数据到第三方表")
public class SyncActiveThirdJob implements SimpleJob {
    public static final Logger logger = LoggerFactory.getLogger(SyncActiveThirdJob.class);
    @Autowired(required = false)
    FeedBackMapper fbMapper;
    @Autowired(required = false)
    Redis redis;
    @Value("${18.feedback.sync-active-last-minutes:10}")
    private Integer expire;
    @Value("${18.feedback.channel}")
    private Integer channel;
    @Value("${spring.application.name}")
    private String appName;

    @Override
    public void execute(ShardingContext shardingContext) {
        logger.info("start SyncActiveThirdJob");
        Date date = new Date();
        Date before = new Date(date.getTime() - TimeUnit.DAYS.toMillis(1));
        long start = date.getTime() - TimeUnit.MINUTES.toMillis(expire);
        Set<String> imeis = new HashSet<>();
        if (redis != null)
            try {
                imeis = redis.zrange("18-day-imeis" + appName, (double) before.getTime(),
                        (double) date.getTime(), () -> fbMapper.getDayImeis(before));
            } catch (Exception e) {
                logger.error(e.getMessage());
                imeis = fbMapper.getDayImeis(before);
            }
        Integer count = fbMapper.syncActiveThird(imeis, new Date(start), channel);
        logger.info("finish SyncActiveThirdJob in : {}ms , count:{}", System.currentTimeMillis() - date.getTime(), count);
    }
}
