package com.eighteen.common.feedback.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by wangwei.
 * Date: 2020/3/17
 * Time: 21:07
 */

@Getter
@Setter
public class RedisData {
    private Boolean isFeedback;

    private Integer weight;

    /**
     * 有效权重
     */
    private Integer effectiveWeight;

    /**
     * 当前权重
     */
    private Integer currentWeight;
}
