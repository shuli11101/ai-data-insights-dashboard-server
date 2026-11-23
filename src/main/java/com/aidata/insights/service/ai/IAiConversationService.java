package com.aidata.insights.service.ai;

import com.aidata.insights.vo.ai.AiConversationDetailVO;
import com.aidata.insights.vo.ai.AiConversationVO;
import com.aidata.insights.vo.ai.DeleteConversationResultVO;

import java.util.List;

public interface IAiConversationService {

    /**
     * 当前用户的会话列表
     */
    List<AiConversationVO> listConversations(int limit);

    /**
     * 会话详情：会话不存在或不属于当前用户时报 404
     */
    AiConversationDetailVO getConversation(String conversationId);

    /**
     * 批量删除会话及其全部消息：只删自己的，其余按 missing 返回
     */
    DeleteConversationResultVO deleteConversations(List<String> conversationIds);
}
