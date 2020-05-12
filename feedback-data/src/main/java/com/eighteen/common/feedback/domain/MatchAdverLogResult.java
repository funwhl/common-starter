package com.eighteen.common.feedback.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author : wangwei
 * @date : 2020/5/12 11:17
 */
@Data
@Accessors(chain = true)
public class MatchAdverLogResult {
    /**
     * 匹配过程中首次匹配的key
     */
    private String matchKey;

    /**
     * 匹配过程中首次匹配的field
     */
    private String matchField;
}
