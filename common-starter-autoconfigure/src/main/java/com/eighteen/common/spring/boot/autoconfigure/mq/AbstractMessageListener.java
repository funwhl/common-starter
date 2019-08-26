package com.eighteen.common.spring.boot.autoconfigure.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 22:40
 */
public abstract class AbstractMessageListener implements MessageListener, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageListener.class);

    private MessageConverter converter = null;
    private RetryTemplate retryTemplate = null;

    @Autowired
    public void setConverter(MessageConverter converter) {
        this.converter = converter;
    }

    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    @Override
    public void onMessage(Message message) {
        com.eighteen.common.spring.boot.autoconfigure.mq.Message msg =
                (com.eighteen.common.spring.boot.autoconfigure.mq.Message) converter.fromMessage(message);
        if (supported(msg.getType())) {
            retryTemplate.execute(e -> {
                onMessage(msg);
                return null;
            });
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (retryTemplate == null) {
            retryTemplate = new RetryTemplate();

            FixedBackOffPolicy backoff = new FixedBackOffPolicy();
            backoff.setBackOffPeriod(5000);
            retryTemplate.setBackOffPolicy(backoff);
            retryTemplate.setRetryPolicy(new SimpleRetryPolicy(5, Collections.singletonMap(Exception.class, true)));

            retryTemplate.registerListener(new RetryListener() {

                @Override
                public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                    return true;
                }

                @Override
                public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                             Throwable throwable) {
                    LOGGER.error("Can not handle message, retry count " + context.getRetryCount(), throwable);
                }

                @Override
                public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                                                           Throwable throwable) {
                    if (throwable == null) return;

                    LOGGER.error("Can not handle message, throw message exception after retied times: " + context.getRetryCount(), throwable);
                    throw new MessageException("message exception: " + throwable.getMessage());
                }
            });
        }

    }

    protected abstract void onMessage(com.eighteen.common.spring.boot.autoconfigure.mq.Message message);

    public abstract int[] getTypes();

    protected boolean supported(int type) {
        return true;
    }
}
