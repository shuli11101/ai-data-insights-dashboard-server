package com.aidata.insights.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏 / 取消收藏请求
 */
@Data
@Schema(description = "收藏请求")
public class FavoriteRequest {

    @Schema(description = "岗位 ID", example = "24014", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请输入岗位 ID")
    private Long jobId;
}
