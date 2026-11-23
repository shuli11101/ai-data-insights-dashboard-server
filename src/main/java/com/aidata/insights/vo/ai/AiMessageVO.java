package com.aidata.insights.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话消息
 *
 * <p>role 只有 user / assistant 两种，按 seq 升序返回，供前端按顺序渲染对话气泡。</p>
 */
@Data
@Schema(description = "AI 会话消息")
public class AiMessageVO {

    @Schema(description = "角色", example = "user")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间", example = "2026-11-07 21:25:51")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}