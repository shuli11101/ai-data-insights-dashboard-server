package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.config.properties.AiProperties;
import com.aidata.insights.dto.ai.AiChatRequest;
import com.aidata.insights.dto.ai.AiReportRequest;
import com.aidata.insights.entity.AiConversation;
import com.aidata.insights.entity.AiMessage;
import com.aidata.insights.mapper.ai.AiConversationMapper;
import com.aidata.insights.mapper.ai.AiMessageMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.ai.IAiChatService;
import com.aidata.insights.vo.ai.AiReportVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI 对话服务实现
 *
 * <p>职责划分：本服务负责鉴权、会话与消息落库、SSE 透传；
 * FastAPI 无状态，只做模型调用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    private static final String CONVERSATION_PREFIX = "c-";

    private static final String ROLE_SYSTEM = "system";

    private static final String ROLE_USER = "user";

    private static final String ROLE_ASSISTANT = "assistant";

    /** 会话标题取首条用户输入的前 20 个字 */
    private static final int TITLE_MAX_LENGTH = 20;

    private static final int CONVERSATION_ID_LENGTH = 17;

    private final AiProperties aiProperties;

    private final AiConversationMapper aiConversationMapper;

    private final AiMessageMapper aiMessageMapper;

    private final RedisService redisService;

    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    /** AI 流式响应耗时长，用独立线程池，避免长期占用 Tomcat 线程 */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostConstruct
    void initHttpClient() {
        // 必须在依赖注入完成后创建，否则 aiProperties 还是 null
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(aiProperties.getConnectTimeout())
                // 必须显式指定 HTTP/1.1：默认 HTTP/2 会对明文 http 先发 h2c 升级请求，
                // uvicorn 不支持该升级，会导致请求体丢失（表现为 422 body missing）
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        // 启动时确认配置：令牌只打长度，不打印明文
        log.info("AI 网关配置: baseUrl={}, internalToken 长度={}, streamTimeout={}",
                aiProperties.getBaseUrl(), aiProperties.getInternalToken().length(),
                aiProperties.getStreamTimeout());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public SseEmitter streamChat(AiChatRequest request) {
        Long userId = SecurityUtils.requireUserId();
        AiConversation conversation = resolveConversation(userId, request);

        // 注意：历史消息要在写入本次用户消息之前读取，避免重复
        List<Map<String, String>> messages = buildMessages(conversation, request.getContent());
        saveMessage(conversation.getId(), nextSeq(conversation.getId()), ROLE_USER, request.getContent());

        SseEmitter emitter = new SseEmitter(aiProperties.getStreamTimeout().toMillis());
        StringBuilder answer = new StringBuilder();
        AtomicBoolean persisted = new AtomicBoolean(false);

        // 断连、超时、异常都要落库，否则用户刷新就丢回答
        Runnable persistAnswer = () -> {
            if (!persisted.compareAndSet(false, true) || answer.isEmpty()) {
                return;
            }
            try {
                saveMessage(conversation.getId(), nextSeq(conversation.getId()),
                        ROLE_ASSISTANT, answer.toString());
                touchConversation(conversation);
            } catch (RuntimeException e) {
                log.error("保存 AI 回复失败: conversationPk={}", conversation.getId(), e);
            }
        };

        emitter.onCompletion(persistAnswer);
        emitter.onTimeout(persistAnswer);
        emitter.onError(e -> persistAnswer.run());

        executor.execute(() -> {
            try {
                streamFromAiService(userId, conversation, messages, delta -> {
                    answer.append(delta);
                    sendEvent(emitter, "delta", Map.of("text", delta));
                });
                sendEvent(emitter, "done", Map.of("conversationId", conversation.getConversationId()));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 流式对话失败: conversationId={}", conversation.getConversationId(), e);
                // 流已开始，无法再改 HTTP 状态码，只能用事件告知
                sendEvent(emitter, "error", Map.of("message", "AI 服务异常，请稍后重试"));
                emitter.complete();
            } finally {
                persistAnswer.run();
            }
        });

        return emitter;
    }

    @Override
    public AiReportVO generateReport(AiReportRequest request) {
        Long userId = SecurityUtils.requireUserId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("scene", request.getScene());
        payload.put("payload", request.getPayload() == null ? Map.of() : request.getPayload());

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder(
                            URI.create(aiProperties.getBaseUrl() + "/internal/report"))
                    .timeout(aiProperties.getSyncTimeout())
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", aiProperties.getInternalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.error("AI 报告生成失败: status={}, body={}", response.statusCode(), response.body());
                throw new BizException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请稍后重试");
            }

            JsonNode node = objectMapper.readTree(response.body());
            AiReportVO vo = new AiReportVO();
            vo.setContent(node.path("content").asText(""));
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 报告生成异常", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请稍后重试");
        }
    }

    /**
     * 调用 FastAPI 的流式接口，逐段回调文本增量
     */
    private void streamFromAiService(Long userId, AiConversation conversation,
                                     List<Map<String, String>> messages,
                                     Consumer<String> onDelta) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("conversationId", conversation.getConversationId());
        payload.put("scene", conversation.getScene());
        payload.put("messages", messages);

        String body = objectMapper.writeValueAsString(payload);
        HttpRequest httpRequest = HttpRequest.newBuilder(
                        URI.create(aiProperties.getBaseUrl() + "/internal/chat/stream"))
                .timeout(aiProperties.getStreamTimeout())
                .header("Content-Type", "application/json")
                .header("X-Internal-Token", aiProperties.getInternalToken())
                // 用会话 ID 作为追踪标识，便于把 Java 与 FastAPI 日志串起来
                .header("X-Trace-Id", conversation.getConversationId())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            // 详情只进日志，不返回给前端
            log.error("AI 服务返回异常状态: status={}, body={}",
                    response.statusCode(), readAllQuietly(response.body()));
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请稍后重试");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 只关心 data 行，event 行由本服务重新命名下发
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(data);
                if (node.hasNonNull("text")) {
                    onDelta.accept(node.get("text").asText());
                }
            }
        }
    }

    /**
     * 解析或创建会话，会话不存在时返回 404
     */
    private AiConversation resolveConversation(Long userId, AiChatRequest request) {
        if (StringUtils.hasText(request.getConversationId())) {
            AiConversation existing = aiConversationMapper.selectOne(
                    Wrappers.<AiConversation>lambdaQuery()
                            .eq(AiConversation::getConversationId, request.getConversationId())
                            .eq(AiConversation::getUserId, userId)
                            .last("LIMIT 1"));
            if (existing == null) {
                throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
            }
            return existing;
        }

        AiConversation created = new AiConversation();
        created.setConversationId(CONVERSATION_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, CONVERSATION_ID_LENGTH));
        created.setUserId(userId);
        created.setScene(request.getScene());
        created.setTitle(truncateTitle(request.getContent()));
        created.setCreateTime(LocalDateTime.now());
        created.setUpdateTime(LocalDateTime.now());
        aiConversationMapper.insert(created);
        return created;
    }

    /**
     * 组装下发给模型的消息：system 提示 + 最近若干条历史 + 本次输入
     */
    private List<Map<String, String>> buildMessages(AiConversation conversation, String current) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.hasText(aiProperties.getSystemPrompt())) {
            Map<String, String> system = new LinkedHashMap<>();
            system.put("role", ROLE_SYSTEM);
            system.put("content", aiProperties.getSystemPrompt());
            messages.add(system);
        }

        List<AiMessage> history = aiMessageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .select(AiMessage::getRole, AiMessage::getContent, AiMessage::getSeq)
                .eq(AiMessage::getConversationPk, conversation.getId())
                .orderByDesc(AiMessage::getSeq)
                .last("LIMIT " + aiProperties.getMaxHistoryMessages()));
        // 查询是倒序取最近 N 条，还原为正序
        Collections.reverse(history);
        for (AiMessage message : history) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent() == null ? "" : message.getContent());
            messages.add(item);
        }

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", ROLE_USER);
        user.put("content", current);
        messages.add(user);
        return messages;
    }

    /**
     * 会话内序号：Redis 原子自增，避免 max(seq)+1 在并发下重号
     */
    private int nextSeq(Long conversationPk) {
        String key = RedisKeyConstant.aiConversationSeq(conversationPk);
        if (!redisService.hasKey(key)) {
            // 首次以库中最大 seq + 1 初始化，兼容既有数据
            AiMessage last = aiMessageMapper.selectOne(Wrappers.<AiMessage>lambdaQuery()
                    .select(AiMessage::getSeq)
                    .eq(AiMessage::getConversationPk, conversationPk)
                    .orderByDesc(AiMessage::getSeq)
                    .last("LIMIT 1"));
            int init = last == null || last.getSeq() == null ? 0 : last.getSeq() + 1;
            redisService.setIfAbsent(key, init,
                    Duration.ofSeconds(RedisKeyConstant.AI_CONVERSATION_SEQ_TTL));
        }
        return (int) redisService.increment(key, 1) - 1;
    }

    private void saveMessage(Long conversationPk, int seq, String role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationPk(conversationPk);
        message.setSeq(seq);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        aiMessageMapper.insert(message);
    }

    private void touchConversation(AiConversation conversation) {
        // update(null, wrapper) 不会触发自动填充，更新时间需显式赋值
        aiConversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                .eq(AiConversation::getId, conversation.getId())
                .set(AiConversation::getUpdateTime, LocalDateTime.now()));
    }

    private void sendEvent(SseEmitter emitter, String name, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            // 客户端已断开属正常场景，这里只降级记录
            log.debug("SSE 推送失败（客户端可能已断开）: event={}", name);
        }
    }

    private String truncateTitle(String content) {
        String text = content == null ? "" : content.trim();
        return text.length() <= TITLE_MAX_LENGTH ? text : text.substring(0, TITLE_MAX_LENGTH);
    }

    private String readAllQuietly(InputStream in) {
        try (InputStream stream = in) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}