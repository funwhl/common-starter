package com.eighteen.common.spring.boot.autoconfigure.swagger;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.*;


/**
 * Created by eighteen.
 * Date: 2019/8/25
 * Time: 11:20
 */

@Configuration
@ConditionalOnProperty(prefix = SwaggerProperties.PREFIX, name = "title")
@EnableConfigurationProperties(SwaggerProperties.class)
@EnableSwagger2
public class SwaggerAutoConfiguration {
    public static final Logger logger = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);
    @Autowired
    SwaggerProperties properties;

    @Bean
    public Docket createRestApi() {
        Set<String> consumes = new HashSet<>();
        consumes.add("application/x-www-form-urlencoded");

        return new Docket(DocumentationType.SWAGGER_2)
                .enable(true)
                .apiInfo(this.apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(properties.getBasePackage()))
                .paths(PathSelectors.any())
                .build()
//                .globalOperationParameters(getTokenPar())
                .consumes(consumes);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(properties.getTitle())//标题
                .termsOfServiceUrl("")
                .contact(new Contact("wangwei", "", ""))
                .description(Optional.ofNullable(properties.getDescription()).orElse(properties.getTitle() + "接口"))
                .version(properties.getVersion())
                .build();
    }


    public List getTokenPar() {
        ParameterBuilder tokenPar = new ParameterBuilder();
        List<Parameter> pars = new ArrayList<>();
        tokenPar.name("Authorization")
                .description("认证信息")
                .modelRef(new ModelRef("string")).parameterType("header").required(true).build();
        pars.add(tokenPar.build());

        return pars;
    }
}
