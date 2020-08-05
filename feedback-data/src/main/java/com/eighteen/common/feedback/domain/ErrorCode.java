package com.eighteen.common.feedback.domain;

import lombok.Getter;

/**
 * @author : wangwei
 * @date : 2020/5/23 16:20
 */
@Getter
public enum ErrorCode {
    first_link_time_out_range(-1, "first link time is out of range not in [today ,1970 ,null]"),
    um_score_checked_failed(100, "um score is less than the config score"),
    call_callback_url_error(101, "call callbackUrl api error"),
    click_channel_unset(101, "click channel unset"),
    channel_type_unset(101, "channel type unset");
    Integer code;
    String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
