package com.eighteen.common.spring.boot.autoconfigure.mq;

/**
 * Created by eighteen.
 * Date: 2019/8/19
 * Time: 21:59
 */
public interface MessageSender {
    void send(int type, Object payload);

    void send(int type, String... headers);

    void send(int type, Object payload, String... headers);

    void send(Message message);
}
