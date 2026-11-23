package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端提交的对话消息（多轮场景使用）
 *
 * <p>role 只允许 user / assistant，system 由服务端按 scene 拼装，
 * 避免客户端伪造 system 覆盖输出规范。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端对话消息")
public class AiChatMessage {

    @Schema(description = "角色：user / assistant", example = "user")
    private String role;

    @Schema(description = "消息内容", example = "帮我分析这个岗位的核心要求")
    private String content;
}
