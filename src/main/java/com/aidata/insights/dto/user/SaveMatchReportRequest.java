package com.aidata.insights.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存匹配报告请求
 */

@Data
@Schema(description="保存匹配报告请求")
public class SaveMatchReportRequest {
    @Schema(description = "岗位 ID", example = "21079", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入岗位 ID")
    private Long jobId;

    @Schema(description = "综合匹配分", example = "18", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入综合匹配分")
    @Min(value=0, message="综合评分不能小于 0")
    @Max(value=100, message="综合评分不能大于 100")
    private Integer score;

    @Schema(description = "匹配报告内容，技能覆盖率、薪资偏差也在正文中"
    , requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入报告内容")
    private String content;
}
