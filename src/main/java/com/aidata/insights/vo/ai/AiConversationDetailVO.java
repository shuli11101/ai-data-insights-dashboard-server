package com.aidata.insights.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 会话详情
 *
 * <p>字段与前端 {@code ConversationDetail} 一一对应；type 由 scene 推导（chat 为 multi），库中无对应列。</p>
 */
@Data
@Schema(description = "AI 会话详情")
public class AiConversationDetailVO {

    @Schema(description = "会话 ID", example = "c-mw6fs2hoea60467f")
    private String conversationId;

    @Schema(description = "会话类型：chat 为 multi，其余为 single", example = "multi")
    private String type;

    @Schema(description = "业务场景", example = "chat")
    private String scene;

    @Schema(description = "最后更新时间", example = "2026-11-07 21:25:51")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "全部消息，按 seq 升序")
    private List<AiMessageVO> messages;
}