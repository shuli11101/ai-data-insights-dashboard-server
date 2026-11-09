package com.aidata.insights.security.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * 签名密钥，长度不得少于 32 个字符（HS256 要求 256 位）
     */
    @NotBlank
    private String secret;

    /**
     * 令牌有效期（分钟）
     */
    @Min(1)
    private long expireMinutes = 720;

    /**
     * 令牌所在的请求头名称
     */
    private String header = "Authorization";

    /**
     * 令牌前缀，注意末尾包含空格
     */
    private String prefix = "Bearer ";
}
