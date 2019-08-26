package com.eighteen.common.spring.boot.autoconfigure.mq;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 22:41
 */
public class MessageException extends RuntimeException {

    public MessageException() {
        super();
    }

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}