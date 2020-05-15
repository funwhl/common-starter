package com.eighteen.common.spring.boot.autoconfigure.pika;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import com.eighteen.common.spring.boot.autoconfigure.redis.RedisTemplateHelper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;

@Configuration
@ConditionalOnClass(RedisClient.class)
@EnableConfigurationProperties(PikaProperties.class)
@ConditionalOnProperty(prefix = PikaProperties.PREFIX, name = "enable")
public class PikaAutoConfiguration extends PikaConnectionConfiguration {
    private final PikaProperties properties;

    private final ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers;

    PikaAutoConfiguration(PikaProperties properties,
                          ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
                          ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
                          ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers) {
        super(properties, sentinelConfigurationProvider, clusterConfigurationProvider);
        this.properties = properties;
        this.builderCustomizers = builderCustomizers;
    }

    /**
     * 默认的clientResources
     *
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    @Primary
    public DefaultClientResources lettuceClientResources() {
        return DefaultClientResources.create();
    }

    /**
     * pika使用的clentResources
     *
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultClientResources pikaClientResources() {
        return DefaultClientResources.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public PikaTemplate pikaTemplate() {
        PikaTemplate pikaTemplate = new PikaTemplate();
        RedisTemplateHelper.setSerializer(pikaTemplate);
        pikaTemplate.setConnectionFactory(redisConnectionFactory(pikaClientResources()));
        return pikaTemplate;
    }

    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
        LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(clientResources,
                this.properties.getLettuce().getPool());
        LettuceConnectionFactory factory = createLettuceConnectionFactory(clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfiguration) {
        if (getSentinelConfig() != null) {
            return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
        }
        if (getClusterConfiguration() != null) {
            return new LettuceConnectionFactory(getClusterConfiguration(), clientConfiguration);
        }
        return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
    }

    private LettuceClientConfiguration getLettuceClientConfiguration(ClientResources clientResources, RedisProperties.Pool pool) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = createBuilder(pool);
        applyProperties(builder);
        if (StringUtils.hasText(this.properties.getUrl())) {
            customizeConfigurationFromUrl(builder);
        }
        builder.clientResources(clientResources);
        customize(builder);
        return builder.build();
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder createBuilder(RedisProperties.Pool pool) {
        if (pool == null) {
            return LettuceClientConfiguration.builder();
        }
        return new PoolBuilderFactory().createBuilder(pool);
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        if (this.properties.isSsl()) {
            builder.useSsl();
        }
        if (this.properties.getTimeout() != null) {
            builder.commandTimeout(this.properties.getTimeout());
        }
        if (this.properties.getLettuce() != null) {
            RedisProperties.Lettuce lettuce = this.properties.getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(this.properties.getLettuce().getShutdownTimeout());
            }
        }
        return builder;
    }

    private void customizeConfigurationFromUrl(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        PikaConnectionConfiguration.ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
        if (connectionInfo.isUseSsl()) {
            builder.useSsl();
        }
    }

    private void customize(LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        this.builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
    }

    /**
     * Inner class to allow optional commons-pool2 dependency.
     */
    private static class PoolBuilderFactory {

        public LettuceClientConfiguration.LettuceClientConfigurationBuilder createBuilder(RedisProperties.Pool properties) {
            return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
        }

        private GenericObjectPoolConfig getPoolConfig(RedisProperties.Pool properties) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(properties.getMaxActive());
            config.setMaxIdle(properties.getMaxIdle());
            config.setMinIdle(properties.getMinIdle());
            if (properties.getTimeBetweenEvictionRuns() != null) {
                config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
            }
            if (properties.getMaxWait() != null) {
                config.setMaxWaitMillis(properties.getMaxWait().toMillis());
            }
            return config;
        }

    }
}
