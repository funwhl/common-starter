package com.eighteen.common.spring.boot.autoconfigure.mq;

import com.eighteen.common.spring.boot.autoconfigure.serializer.HessianSerializer;
import com.eighteen.common.spring.boot.autoconfigure.serializer.Serializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 23:39
 */
@Configuration
public class RabbitMqAutoConfiguration {
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
}
