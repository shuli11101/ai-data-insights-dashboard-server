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
     * 同步长请求超时（报告生成等）
     */
    private Duration syncTimeout = Duration.ofSeconds(150);

    /**
     * 每次带给模型的历史消息条数上限
     */
    @Min(1)
    private int maxHistoryMessages = 20;

    /**
     * 系统提示词，为空时不下发 system 消息
     */
    private String systemPrompt = "你是招聘数据分析助手，回答要简洁、准确，必要时用 Markdown 输出。";
}