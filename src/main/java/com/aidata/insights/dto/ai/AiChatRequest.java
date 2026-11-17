package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 流式对话请求
 */
@Data
@Schema(description = "AI 流式对话请求")
public class AiChatRequest {

    @Schema(description = "会话 ID，首轮不传则由服务端创建", example = "c-mw6fs2hoea60467f")
    @Size(max = 64, message = "会话 ID 过长")
    private String conversationId;

    @Schema(description = "业务场景，如 jd", example = "jd")
    @Size(max = 32, message = "场景标识过长")
    private String scene;

    @Schema(description = "用户输入内容", example = "帮我分析这个岗位的核心要求", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入内容")
    @Size(max = 2000, message = "单次输入不能超过 2000 字")
    private String content;
}