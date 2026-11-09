package com.aidata.insights.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置项
 */
@Data
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

    /**
     * 允许的来源，生产环境应配置为具体域名
     */
    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    private List<String> allowedHeaders = List.of("*");

    private boolean allowCredentials = true;

    /**
     * 预检请求缓存时间（秒）
     */
    private long maxAge = 3600;
}
