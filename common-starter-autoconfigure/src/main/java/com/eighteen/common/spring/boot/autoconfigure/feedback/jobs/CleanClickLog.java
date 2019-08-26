package com.eighteen.common.spring.boot.autoconfigure.feedback.jobs;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
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
 * @date : 2019/8/23 11:08
 */

@TaskJob(jobName = "CleanClickLog", cron = "${18.feedback.cliean-click-cron:0 0 1 1/1 * ? }", desc = "清理点击记录表")
public class CleanClickLog implements SimpleJob {
    private static final Logger logger = LoggerFactory.getLogger(CleanClickLog.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    @Value("${18.feedback.click-data-expire:7}")
    private Integer clickExpire;

    @Override
    public void execute(ShardingContext shardingContext) {
        logger.info("start clean CleanClickLog");
        Long start = System.currentTimeMillis();
        feedBackMapper.cleanClickLog(new Date(start - TimeUnit.DAYS.toMillis(clickExpire)));
        logger.info("finish clean CleanClickLog in : {}ms", System.currentTimeMillis() - start);
    }
}
