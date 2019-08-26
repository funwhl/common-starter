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
import java.util.concurrent.TimeUnit;

/**
 * @author : eighteen
 * @date : 2019/8/23 11:25
 * (jobName = "CleanDayImei",shardingTotalCount = 1,
 * shardingItemParameters = "0=a,1=b,2=c,3=d,4=f,5=g,6=h,7=i,8=j,9=k", cron = "${18.feedback.clean-imeis-cron:1/5 * * * * ?}", desc = "清理imei表")
 */

@TaskJob(jobName = "CleanDayImei", cron = "${18.feedback.clean-imeis-cron:0 0 1 1/1 * ? }", desc = "清理imei表")
public class CleanImeis implements SimpleJob {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackJob.class);
    @Autowired(required = false)
    FeedBackMapper fbMapper;
    @Autowired(required = false)
    Redis redis;
    @Value("${spring.application.name}")
    private String appName;

    @Override
    public void execute(ShardingContext shardingContext) {
        logger.info("start clean DayImei");
        Long start = System.currentTimeMillis();
        long offset = start - TimeUnit.DAYS.toMillis(1);
        if (redis != null) try {
            redis.zexpire("18-day-imeis" + appName,
                    0d, (double) offset);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        fbMapper.cleanDayImeis(new Date(offset));
        logger.info("finish clean DayImei in : {}ms", System.currentTimeMillis() - start);
    }
}
