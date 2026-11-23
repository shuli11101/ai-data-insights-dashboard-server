package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 岗位匹配度分析请求
 */
@Data
@Schema(description = "岗位匹配度分析请求")
public class AiMatchRequest {

    @Schema(description = "岗位 ID", example = "24014", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入岗位 ID")
    @Positive(message = "岗位 ID 不正确")
    private Long jobId;

    @Schema(description = "true 时忽略已保存的报告，重新生成", example = "false")
    private Boolean force;
}