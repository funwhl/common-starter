package com.eighteen.common.feedback.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author lcomplete
 */
@Data
@Accessors(chain = true)
public class MatchClickLogResult {
    private String clickType;

    private Long clickLogId;

    /**
     * 匹配过程中首次匹配的key
     */
    private String matchKey;

    /**
     * 匹配过程中首次匹配的field
     */
    private String matchField;

}
