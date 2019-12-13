package com.eighteen.common.spring.boot.autoconfigure.rabbitmq;

import com.eighteen.common.mq.rabbitmq.MessageException;
import com.eighteen.common.mq.rabbitmq.MessageSender;
import com.eighteen.common.spring.boot.autoconfigure.rabbitmq.consumer.AbstractMessageListener;
import com.eighteen.common.spring.boot.autoconfigure.rabbitmq.product.MessageSenderFactory;
import com.eighteen.common.serializer.hessian.HessianSerializer;
import com.eighteen.common.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ErrorHandler;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by wangwei.
 * Date: 2019/8/21
 * Time: 23:39
 */
@Configuration
public class RabbitMqAutoConfiguration implements EnvironmentAware, BeanFactoryAware, InitializingBean, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqAutoConfiguration.class);
    private ApplicationContext context;
    private Environment env;
    private DefaultListableBeanFactory beanFactory;
    private int prefetchCount = 1;
    private int concurrentConsumers = 1;
    private int maxConcurrentConsumers = 1;
    private String envName;

    @Bean
    @ConditionalOnMissingBean
    public Serializer serializer() {
        return new HessianSerializer();
    }

    @Bean
    public CommonMessageConverter commonMessageConverter(Serializer serializer) {
        CommonMessageConverter converter = new CommonMessageConverter();
        converter.setSerializer(serializer);
        return converter;
    }

    @Bean
    public MessageSenderFactory messageSenderFactory() {
        return new MessageSenderFactory();
    }

    @Bean
    public MessageSender messageSender(MessageSenderFactory messageSenderFactory, Environment env) {
        return messageSenderFactory.create(env.getProperty("spring.rabbitmq.default-exchange"));
    }

    @Bean
    public DirectExchange exchange(Environment env) {
        return new DirectExchange(env.getProperty("spring.rabbitmq.default-exchange"));
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof DefaultListableBeanFactory) {
            this.beanFactory = (DefaultListableBeanFactory) beanFactory;
        }
    }


    @Override
    public void setEnvironment(Environment env) {
        this.env = env;
        prefetchCount = env.getProperty("spring.rabbitmq.listener.simple.prefetch", Integer.class, SimpleMessageListenerContainer.DEFAULT_PREFETCH_COUNT);
        concurrentConsumers = env.getProperty("spring.rabbitmq.listener.simple.concurrency", Integer.class, 1);
        maxConcurrentConsumers = env.getProperty("spring.rabbitmq.listener.simple.max-concurrency", Integer.class, 1);
        envName = env.getProperty("application.env.name", String.class, "dev");

    }

    private SimpleMessageListenerContainer createListener(ConnectionFactory connectionFactory, MessageListener listener, String queue) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setPrefetchCount(prefetchCount);
        container.setConcurrentConsumers(concurrentConsumers);
        container.setMaxConcurrentConsumers(maxConcurrentConsumers);
        container.setQueueNames(queue);
        container.setMessageListener(listener);
        container.setErrorHandler(new ErrorHandler() {
            @Override
            public void handleError(Throwable throwable) {
                if (causeChainContainsARADRE(throwable)) {
                    StringBuilder errorMessage = new StringBuilder(envName);
                    errorMessage.append("环境异步监听服务出错: ");
                    errorMessage.append(throwable.getCause().getMessage());
                    LOGGER.error("message listener shutdown: " + throwable.getCause().getMessage());
                    // 发送邮件消息
//                    messageSender.send(MessageTypeContent.SEND_MESSAGE_WARNING_EMAIL, (Object)errorMessage.toString(),
//                            MessageHeaderContent.SUBJECT, "human-resource project " + listener.getClass().getSimpleName() + "服务挂起");
                    // 停止监听
//                    container.shutdown();
                }
            }

            private boolean causeChainContainsARADRE(Throwable t) {
                for (Throwable cause = t.getCause(); cause != null; cause = cause.getCause()) {
                    if (cause instanceof MessageException) {
                        return true;
                    }
                }
                return false;
            }
        });
        return container;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
        Map<String, AbstractMessageListener> map = context.getBeansOfType(AbstractMessageListener.class);
        map.forEach((o, o2) -> {
            AbstractMessageListener listener = o2;
            String name = o;
            String queueName = name + "_queue";
            Queue queue = new Queue(queueName, true);
            this.beanFactory.registerSingleton(queueName, queue);
            Arrays.stream(listener.getTypes()).forEach(type -> {
                this.beanFactory.registerSingleton(name + "#" + type,
                        BindingBuilder.bind(queue).to(exchange(env)).with(String.valueOf(type)));
            });
            SimpleMessageListenerContainer container = createListener(connectionFactory, listener, queueName);
            this.beanFactory.registerSingleton(name + "Container", container);
        });
    }
}
