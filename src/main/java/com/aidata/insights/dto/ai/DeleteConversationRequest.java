package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除 AI 会话请求
 *
 * <p>单次最多 50 个，与历史服务保持一致，避免超长 IN 查询。</p>
 */
@Data
@Schema(description = "批量删除 AI 会话请求")
public class DeleteConversationRequest {

    @Schema(description = "会话 ID 列表，单次 1-50 个", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "conversationIds 不能为空")
    @Size(max = 50, message = "单次最多删除 50 个会话")
    private List<String> conversationIds;
}