package com.aidata.insights.service.ai;

import com.aidata.insights.entity.AiConversation;
import com.aidata.insights.entity.AiMessage;
import com.aidata.insights.mapper.ai.AiConversationMapper;
import com.aidata.insights.mapper.ai.AiMessageMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.vo.ai.AiConversationVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 会话服务
 *
 * <p>会话列表按最后更新时间倒序；消息条数与最后一条消息由 ai_message 批量统计得出。</p>
 */
@Service
@RequiredArgsConstructor
public class AiConversationService {

    /** 历史服务约定：会话类型固定 single，库中没有对应列 */
    private static final String DEFAULT_TYPE = "single";

    /** limit 上限，防止一次拉取过多消息 */
    private static final int MAX_LIMIT = 200;

    private final AiConversationMapper aiConversationMapper;

    private final AiMessageMapper aiMessageMapper;

    /**
     * 当前用户的会话列表
     */
    public List<AiConversationVO> listConversations(int limit) {
        Long userId = SecurityUtils.requireUserId();
        int size = Math.min(Math.max(limit, 1), MAX_LIMIT);

        List<AiConversation> conversations = aiConversationMapper.selectList(
                Wrappers.<AiConversation>lambdaQuery()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdateTime)
                        .orderByDesc(AiConversation::getId)
                        .last("LIMIT " + size));
        if (conversations.isEmpty()) {
            return List.of();
        }

        List<Long> conversationPks = conversations.stream().map(AiConversation::getId).toList();
        Map<Long, List<AiMessage>> messagesByPk = aiMessageMapper.selectList(
                        Wrappers.<AiMessage>lambdaQuery()
                                .select(AiMessage::getConversationPk, AiMessage::getSeq, AiMessage::getContent)
                                .in(AiMessage::getConversationPk, conversationPks)
                                .orderByAsc(AiMessage::getSeq)
                                .orderByAsc(AiMessage::getId))
                .stream()
                .collect(Collectors.groupingBy(AiMessage::getConversationPk));

        return conversations.stream()
                .map(conversation -> toVO(conversation, messagesByPk.get(conversation.getId())))
                .toList();
    }

    private AiConversationVO toVO(AiConversation conversation, List<AiMessage> messages) {
        AiConversationVO vo = new AiConversationVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setType(DEFAULT_TYPE);
        vo.setScene(conversation.getScene());
        vo.setTitle(conversation.getTitle());
        vo.setUpdateTime(conversation.getUpdateTime());
        vo.setMessageCount(messages == null ? 0 : messages.size());
        if (messages != null && !messages.isEmpty()) {
            // 已按 seq 升序取出，最后一条即最新消息
            vo.setLastMessage(messages.get(messages.size() - 1).getContent());
        }
        return vo;
    }
}
