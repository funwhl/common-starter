package com.eighteen.common.feedback.domain;

import lombok.Data;

/**
 * @author lcomplete
 */
@Data
public class MatchClickLogResult {
    private String channelType;

    private Long clickLogId;

    /**
     * 匹配过程中首次匹配的key
     */
    private String matchKey;

    /**
     * 匹配过程中首次匹配的field
     */
    private String matchField;

    /**
     * 之前是否匹配回传过
     */
    private boolean matchedBefore;
}
