package com.aidata.insights.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量删除 AI 会话结果
 *
 * <p>missing 返回不存在或不属于当前用户的 ID，前端可据此提示「部分会话已不存在」。</p>
 */
@Data
@Schema(description = "批量删除 AI 会话结果")
public class DeleteConversationResultVO {

    @Schema(description = "实际删除的会话数", example = "2")
    private int deleted;

    @Schema(description = "不存在或不属于当前用户的会话 ID")
    private List<String> missing;
}