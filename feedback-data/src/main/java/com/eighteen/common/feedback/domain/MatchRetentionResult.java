package com.eighteen.common.feedback.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : wangwei
 * @date : 2020/5/6 17:29
 */
@Data
@Accessors(chain = true)
public class MatchRetentionResult {
    private String clickType;

    private Long clickLogId;

    private String feedbackDate;
}
