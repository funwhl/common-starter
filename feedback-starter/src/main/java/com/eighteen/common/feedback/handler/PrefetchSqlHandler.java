package com.eighteen.common.feedback.handler;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.eighteen.common.feedback.EighteenProperties;
import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * Created by wangwei.
 * Date: 2020/3/26
 * Time: 15:56
 */
public interface PrefetchSqlHandler {
    BooleanExpression handler(EighteenProperties etprop,BooleanExpression expression,ShardingContext sc);
}
