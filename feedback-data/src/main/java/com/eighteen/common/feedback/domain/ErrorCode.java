package com.eighteen.common.feedback.domain;

import lombok.Getter;

/**
 * @author : wangwei
 * @date : 2020/5/23 16:20
 */
@Getter
public enum ErrorCode {
    first_link_time_out_range(-1, "first link time is out of range not in [today ,1970 ,null]");
    Integer code;
    String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
