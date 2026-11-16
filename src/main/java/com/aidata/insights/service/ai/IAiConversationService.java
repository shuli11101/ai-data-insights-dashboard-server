package com.aidata.insights.service.ai;

import com.aidata.insights.vo.ai.AiConversationVO;

import java.util.List;

public interface IAiConversationService {

    /**
     * 当前用户的会话列表
     */
    List<AiConversationVO> listConversations(int limit);
}
