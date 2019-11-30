package com.eighteen.base.mq.rabbitmq;

/**
 * Created by wangwei.
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