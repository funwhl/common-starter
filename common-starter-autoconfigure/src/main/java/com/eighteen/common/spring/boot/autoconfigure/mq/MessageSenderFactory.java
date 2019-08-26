package com.eighteen.common.spring.boot.autoconfigure.mq;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 22:39
 */
public class MessageSenderFactory {

    private RabbitTemplate rabbitTemplate = null;
    private MessageConverter messageConverter = null;

    @Autowired
    public void setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public MessageSender create(String exchange) {
        return new DefaultMessageSender(rabbitTemplate, exchange, messageConverter);
    }

    public static class DefaultMessageSender implements MessageSender {

        private String exchange = null;
        private RabbitTemplate rabbitTemplate = null;
        private MessageConverter converter = null;

        public DefaultMessageSender(RabbitTemplate template, String exchange, MessageConverter converter) {
            this.rabbitTemplate = template;
            this.exchange = exchange;
            this.converter = converter;
        }

        @Override
        public void send(int type, Object payload) {
            send(Message.create(type, payload));
        }

        @Override
        public void send(int type, Object payload, String... headers) {
            send(Message.create(type, payload, headers));
        }

        @Override
        public void send(int type, String... headers) {
            send(Message.create(type, null, headers));
        }

        @Override
        public void send(Message message) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {

                    @Override
                    public void afterCommit() {
                        try {
                            rabbitTemplate.send(exchange, String.valueOf(message.getType()), converter.toMessage(message, new MessageProperties()));
                        } catch (Exception e) {
                            // TODO handle exception and retry to confirm message will be sent finally
                            e.printStackTrace();
                        }

                    }

                });
                return;
            }
            rabbitTemplate.send(exchange, String.valueOf(message.getType()), converter.toMessage(message, new MessageProperties()));
        }

    }

}
