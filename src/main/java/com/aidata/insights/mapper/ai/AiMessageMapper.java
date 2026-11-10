package com.aidata.insights.mapper.ai;

import com.aidata.insights.entity.AiMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话消息 Mapper
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
