package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话（表 ai_conversation）
 *
 * <p>conversation_id 是对外的字符串 ID，id 是内部主键，ai_message 通过 conversation_pk 关联。</p>
 */
@Data
@TableName("ai_conversation")
public class AiConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对外会话 ID，如 c-mw6fs2hoea60467f
     */
    private String conversationId;

    private Long userId;

    /**
     * 业务场景，如 jd
     */
    private String scene;

    private String title;

    private String clientIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
