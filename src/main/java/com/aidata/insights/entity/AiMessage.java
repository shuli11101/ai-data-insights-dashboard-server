package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话消息（表 ai_message）
 */
@Data
@TableName("ai_message")
public class AiMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话主键，对应 ai_conversation.id
     */
    private Long conversationPk;

    /**
     * 会话内序号，从 0 开始递增
     */
    private Integer seq;

    /**
     * 角色：user / assistant
     */
    private String role;

    private String content;

    private LocalDateTime createTime;
}
