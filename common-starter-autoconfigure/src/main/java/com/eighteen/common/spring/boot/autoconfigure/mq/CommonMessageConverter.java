package com.eighteen.common.spring.boot.autoconfigure.mq;

import com.eighteen.common.spring.boot.autoconfigure.serializer.Serializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 22:40
 */
public class CommonMessageConverter implements MessageConverter {

    private Serializer serializer = null;

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        return serializer.read(message.getBody(), com.eighteen.common.spring.boot.autoconfigure.mq.Message.class);
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        return new Message(serializer.write(object), messageProperties);
    }
}

