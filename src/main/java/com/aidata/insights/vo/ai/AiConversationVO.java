package com.aidata.insights.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话列表项
 *
 * <p>字段按前端契约输出驼峰；type 为历史约定值，messageCount/lastMessage 来自 ai_message。</p>
 */
@Data
@Schema(description = "AI 会话列表项")
public class AiConversationVO {

    @Schema(description = "会话 ID", example = "c-mw6fs2hoea60467f")
    private String conversationId;

    @Schema(description = "会话类型", example = "single")
    private String type;

    @Schema(description = "业务场景", example = "jd")
    private String scene;

    @Schema(description = "会话标题", example = "成都在线班主任招聘")
    private String title;

    @Schema(description = "最后更新时间", example = "2026-11-07 21:25:51")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "消息条数", example = "2")
    private Integer messageCount;

    @Schema(description = "最后一条消息内容")
    private String lastMessage;
}
