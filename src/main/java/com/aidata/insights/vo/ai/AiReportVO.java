package com.aidata.insights.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 同步长请求结果
 */
@Data
@Schema(description = "AI 同步长请求结果")
public class AiReportVO {

    @Schema(description = "生成的正文，通常为 Markdown")
    private String content;
}