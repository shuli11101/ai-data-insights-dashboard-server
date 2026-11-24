package com.aidata.insights.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AI 流式对话请求
 *
 * <p>两种写法二选一，与前端契约保持一致：</p>
 * <ul>
 *   <li>多轮：{@code scene + messages}，messages 为完整历史，最后一条必须是 user</li>
 *   <li>单轮：{@code scene + input}，可选 {@code context} 作为补充信息</li>
 * </ul>
 *
 * <p>{@code content} 仅用于兼容旧调用方，新调用请使用 input。</p>
 */
@Data
@Schema(description = "AI 流式对话请求")
public class AiChatRequest {

    @Schema(description = "会话 ID，首轮不传则由服务端创建", example = "c-mw6fs2hoea60467f")
    @Size(max = 64, message = "会话 ID 过长")
    private String conversationId;

    @Schema(description = "业务场景：jdDesc / skill / jd / chat", example = "jd")
    @Size(max = 32, message = "场景标识过长")
    private String scene;

    @Schema(description = "多轮对话消息（scene=chat 使用，与 input 二选一）")
    private List<AiChatMessage> messages;

    @Schema(description = "单轮输入内容（与 messages 二选一）", example = "帮我分析这个岗位的核心要求")
    @Size(max = 8000, message = "单次输入不能超过 8000 字")
    private String input;

    @Schema(description = "补充信息（岗位详情、求职者信息等），拼在本轮用户输入之后")
    @Size(max = 8000, message = "补充信息不能超过 8000 字")
    private String context;

    /**
     * 兼容字段：旧调用方用 content 传单轮输入
     */
    @Schema(description = "用户输入内容（兼容字段，建议改用 input）")
    @Size(max = 8000, message = "单次输入不能超过 8000 字")
    private String content;
}