package com.eighteen.common.feedback.service;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.eighteen.common.feedback.domain.ThrowChannelConfig;
import com.eighteen.common.feedback.entity.ActiveLogger;
import com.eighteen.common.feedback.entity.DayHistory;
import com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.List;
import java.util.Map;

/**
 * @author : eighteen
 * @date : 2019/8/22 17:42
 */

public interface FeedbackService {
    void feedback(ShardingContext c,Boolean cold);

    List<ActiveLogger> getPrefetchList(String[] sd, Map.Entry<String, BooleanExpression> e, BooleanExpression expression);

    List<ThrowChannelConfig> getThrowChannelConfigs();

    void syncActive(ShardingContext c);

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
