package com.eighteen.common.spring.boot.autoconfigure.swagger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import springfox.documentation.builders.ApiInfoBuilder;

/**
 * Created by eighteen.
 * Date: 2019/8/18
 * Time: 10:45
 */
@Data
@ConfigurationProperties(prefix = SwaggerProperties.PREFIX)
public class SwaggerProperties {
    public static final String PREFIX = "18.swagger";
    private String basePackage = "com.eighteen";
    private String title = "";
    private String description;
    private String version;
    private String termsOfServiceUrl;
}
