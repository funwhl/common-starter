package com.eighteen.common.feedback.domain;

import lombok.Data;

/**
 * @author lcomplete
 */
@Data
public class MatchNewUserRetryResult {
    private String dataSource;

    private Long newUserRetryId;
}
