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
 * @date : 2019/8/23 11:09
 */

@TaskJob(jobName = "TransferActiveT2History", cron = "${18.feedback.transfer-active-cron:0 0 1 1/1 * ? }", desc = "移动第三方激活数据到历史表")
public class TransferActiveT2History implements SimpleJob {
    public static final Logger logger = LoggerFactory.getLogger(TransferActiveT2History.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    @Value("${18.feedback.channel-active2history:2}")
    Integer expire;

    @Override
    public void execute(ShardingContext shardingContext) {
        logger.info("start TransferActiveT2History");
        Date start = new Date();
        Date before = new Date(start.getTime() - TimeUnit.DAYS.toMillis(expire));
        Integer to = feedBackMapper.transferActiveToHistory(before);
        Integer from = feedBackMapper.deleteActiveThird(before);
        logger.info("finish TransferActiveT2History in : {}ms , to:{},from:{}", System.currentTimeMillis() - start.getTime(), to, from);
    }
}
