package com.aidata.insights.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * AI 服务（FastAPI）配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * FastAPI 服务地址，仅内网可达
     */
    @NotBlank
    private String baseUrl = "http://127.0.0.1:8000";

    /**
     * 服务间调用令牌，必须与 FastAPI 的 AI_INTERNAL_TOKEN 一致
     */
    @NotBlank
    private String internalToken = "change-me";

    /**
     * 建立连接超时
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * 流式请求超时，需大于 FastAPI 侧 request-timeout
     */
    private Duration streamTimeout = Duration.ofSeconds(180);

    /**
     * 每次带给模型的历史消息条数上限
     */
    @Min(1)
    private int maxHistoryMessages = 20;

    /**
     * 系统提示词：默认空，由 FastAPI 按 scene 下发；
     * 仅在需要紧急覆盖时配置，非空时 FastAPI 会让位。
     */
    private String systemPrompt = "";
}