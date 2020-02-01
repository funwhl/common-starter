package com.eighteen.common.mq.rabbitmq;

/**
 * Created by wangwei.
 * Date: 2019/8/19
 * Time: 21:59
 */
public interface MessageSender {
    void send(String type, Object payload);

    void send(String type, String... headers);

    void send(String type, Object payload, String... headers);

    void send(Message message);
}
