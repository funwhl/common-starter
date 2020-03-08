package com.eighteen.common.feedback.service;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.eighteen.common.feedback.entity.DayHistory;
import com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType;

import java.util.List;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:42
 */

public interface FeedbackService {
    void feedback(ShardingContext c);

    void syncActive(ShardingContext c) throws InterruptedException;

    void clean(JobType type,ShardingContext c);

    void clearCache(Long offset);

    void syncCache();

    void stat(JobType type,ShardingContext c);

    /**
     * 次日留存
     * @param type
     */
    void secondStay(JobType type,ShardingContext c);

    String getDayCacheRedisKey(String key);

    String generMember(Integer coid,Integer ncoid, String value);

    void addDayCache(String key, List<DayHistory> dayHistories);

    List<DayHistory> getDayCache(String key);

    Long fb();
}
