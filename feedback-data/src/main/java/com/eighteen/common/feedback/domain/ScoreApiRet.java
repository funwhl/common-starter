package com.eighteen.common.feedback.domain;

import lombok.Data;

/**
 * @author : wangwei
 * @date : 2020/5/25 19:12
 */
@Data
public class ScoreApiRet {
    Integer errorCode;
    String errorMsg;
    Boolean success;
    Result result;

    @Data
    public class Result {
        String score;
        Boolean suc;
    }
}
