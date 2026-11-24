package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.entity.AiConversation;
import com.aidata.insights.entity.AiMessage;
import com.aidata.insights.mapper.ai.AiConversationMapper;
import com.aidata.insights.mapper.ai.AiMessageMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.ai.IAiConversationService;
import com.aidata.insights.vo.ai.AiConversationDetailVO;
import com.aidata.insights.vo.ai.AiConversationVO;
import com.aidata.insights.vo.ai.AiMessageVO;
import com.aidata.insights.vo.ai.DeleteConversationResultVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 会话服务
 *
 * <p>会话列表按最后更新时间倒序；消息条数与最后一条消息由 ai_message 批量统计得出。</p>
 */
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements IAiConversationService {

    /** chat 是多轮对话，其余场景（jdDesc / skill / jd）都是一次性单轮分析 */
    private static final String SCENE_CHAT = "chat";

    /** 单轮分析 */
    private static final String TYPE_SINGLE = "single";

    /** 多轮对话 */
    private static final String TYPE_MULTI = "multi";

    /** limit 上限，防止一次拉取过多消息 */
    private static final int MAX_LIMIT = 200;

    /** 单次批量删除上限，避免超长 IN 查询 */
    private static final int MAX_BATCH_DELETE = 50;

    /** 会话 ID 允许的字符与长度，与写入侧保持一致 */
    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final AiConversationMapper aiConversationMapper;

    private final AiMessageMapper aiMessageMapper;

    /**
     * 当前用户的会话列表
     */
    @Override
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
        vo.setType(resolveType(conversation.getScene()));
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

    /**
     * 会话详情：只允许查自己的会话，按 seq 升序返回全部消息
     */
    @Override
    public AiConversationDetailVO getConversation(String conversationId) {
        Long userId = SecurityUtils.requireUserId();
        String id = conversationId == null ? null : conversationId.trim();
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.BAD_REQUEST, "conversationId 参数错误");
        }

        // user_id 条件保证拿不到别人的会话
        AiConversation conversation = aiConversationMapper.selectOne(
                Wrappers.<AiConversation>lambdaQuery()
                        .eq(AiConversation::getConversationId, id)
                        .eq(AiConversation::getUserId, userId)
                        .last("LIMIT 1"));
        if (conversation == null) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }

        List<AiMessage> messages = aiMessageMapper.selectList(
                Wrappers.<AiMessage>lambdaQuery()
                        .eq(AiMessage::getConversationPk, conversation.getId())
                        .orderByAsc(AiMessage::getSeq)
                        .orderByAsc(AiMessage::getId));

        AiConversationDetailVO vo = new AiConversationDetailVO();
        vo.setConversationId(conversation.getConversationId());
        vo.setType(resolveType(conversation.getScene()));
        vo.setScene(conversation.getScene());
        vo.setUpdateTime(conversation.getUpdateTime());
        vo.setMessages(messages.stream().map(this::toMessageVO).toList());
        return vo;
    }

    /**
     * type 不落库，按 scene 推导：chat 是多轮对话，其余都是单轮分析
     */
    private String resolveType(String scene) {
        return SCENE_CHAT.equals(scene) ? TYPE_MULTI : TYPE_SINGLE;
    }

    /**
     * 批量删除会话及其全部消息：只删自己的，别人的按 missing 返回
     */
    @Override
    @Transactional
    public DeleteConversationResultVO deleteConversations(List<String> rawConversationIds) {
        Long userId = SecurityUtils.requireUserId();
        List<String> conversationIds = normalizeConversationIds(rawConversationIds);
        if (conversationIds.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "conversationIds 参数错误（1-" + MAX_BATCH_DELETE + " 个合法 ID）");
        }

        List<AiConversation> existing = aiConversationMapper.selectList(
                Wrappers.<AiConversation>lambdaQuery()
                        .select(AiConversation::getId, AiConversation::getConversationId)
                        .in(AiConversation::getConversationId, conversationIds)
                        .eq(AiConversation::getUserId, userId));
        if (existing.isEmpty()) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }

        List<Long> conversationPks = existing.stream().map(AiConversation::getId).toList();
        Set<String> foundIds = existing.stream()
                .map(AiConversation::getConversationId)
                .collect(Collectors.toSet());

        // 先删消息再删会话，避免留下孤立消息；会话侧再带一次 user_id 防越权
        aiMessageMapper.delete(Wrappers.<AiMessage>lambdaQuery()
                .in(AiMessage::getConversationPk, conversationPks));
        aiConversationMapper.delete(Wrappers.<AiConversation>lambdaQuery()
                .in(AiConversation::getId, conversationPks)
                .eq(AiConversation::getUserId, userId));

        DeleteConversationResultVO vo = new DeleteConversationResultVO();
        vo.setDeleted(conversationPks.size());
        vo.setMissing(conversationIds.stream().filter(id -> !foundIds.contains(id)).toList());
        return vo;
    }

    /**
     * 去重并校验会话 ID：任何一个不合法都整体判为参数错误，与历史服务行为一致
     */
    private List<String> normalizeConversationIds(List<String> rawConversationIds) {
        if (rawConversationIds == null || rawConversationIds.isEmpty()
                || rawConversationIds.size() > MAX_BATCH_DELETE) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String item : rawConversationIds) {
            String id = item == null ? null : item.trim();
            if (id == null || !CONVERSATION_ID_PATTERN.matcher(id).matches()) {
                return List.of();
            }
            ids.add(id);
        }
        return List.copyOf(ids);
    }

    private AiMessageVO toMessageVO(AiMessage message) {
        AiMessageVO vo = new AiMessageVO();
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
