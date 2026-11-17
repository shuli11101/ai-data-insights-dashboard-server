package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * AI 同步长请求（报告生成、简历解析等）
 */
@Data
@Schema(description = "AI 同步长请求")
public class AiReportRequest {

    @Schema(description = "业务场景", example = "match-report", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "缺少场景标识")
    @Size(max = 32, message = "场景标识过长")
    private String scene;

    @Schema(description = "业务参数，原样透传给 AI 服务")
    private Map<String, Object> payload;
}