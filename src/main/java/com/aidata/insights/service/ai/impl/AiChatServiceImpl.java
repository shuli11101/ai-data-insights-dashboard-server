package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.common.trace.TraceContext;
import com.aidata.insights.config.properties.AiProperties;
import com.aidata.insights.dto.ai.AiChatMessage;
import com.aidata.insights.dto.ai.AiChatRequest;
import com.aidata.insights.entity.AiConversation;
import com.aidata.insights.entity.AiMessage;
import com.aidata.insights.mapper.ai.AiConversationMapper;
import com.aidata.insights.mapper.ai.AiMessageMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.ai.AiStreamClient;
import com.aidata.insights.service.ai.IAiChatService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * AI 对话服务实现
 *
 * <p>
 * 职责划分：本服务负责鉴权、消息校验、会话与消息落库、SSE 透传；
 * FastAPI 无状态，只做模型调用。对外事件格式与前端契约一致：
 * </p>
 * <ul>
 * <li>首帧 {@code data:{"conversationId":"c-xxx"}}</li>
 * <li>增量 {@code data:{"content":"..."}}</li>
 * <li>结束 {@code data:[DONE]}</li>
 * <li>异常 {@code data:{"error":"..."}}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

    // 会话ai前缀
    private static final String CONVERSATION_PREFIX = "c-";

    // 角色定义
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    // 场景定义
    private static final String SCENE_CHAT = "chat";
    private static final String SCENE_SKILL = "skill";
    private static final String SCENE_JD = "jd";

    /** 会话标题取首条用户输入的前 24 个字 */
    private static final int TITLE_MAX_LENGTH = 10;

    /** 会话 ID 长度 */
    private static final int CONVERSATION_ID_LENGTH = 17;

    /** 单次请求最多携带的消息条数，避免超长上下文打爆上游 */
    private static final int MAX_MESSAGES = 30;

    /** 单条消息字符上限 */
    private static final int MAX_CONTENT_LENGTH = 8000;

    /** 上下文字符上限 */
    private static final int MAX_TOTAL_LENGTH = 30000;

    /** 线程池饱和时的提示文案：流已开始，只能以 error 帧下发 */
    private static final String BUSY_MESSAGE = "当前使用人数较多，请稍后再试";

    /** 自动场景名，match 只能由 /ai/match 使用，不能走本接口 */
    private static final Set<String> SCENES = Set.of("drawer", "interpret", "skill", "jd", SCENE_CHAT);

    /** 前端历史版本可能传数字编码：0=drawer 1=interpret 2=skill 3=jd 4=chat */
    private static final Map<String, String> SCENE_CODES = Map.of(
            "0", "drawer",
            "1", "interpret",
            "2", SCENE_SKILL,
            "3", SCENE_JD,
            "4", SCENE_CHAT);

    /** 会话 ID 允许的字符与长度，与旧服务一致 */
    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /** fastapi侧配置项 */
    private final AiProperties aiProperties;

    /** AI 流式接口客户端 */
    private final AiStreamClient aiStreamClient;

    /** 会话Mapper，用来操作 ai_conversation 表 */
    private final AiConversationMapper aiConversationMapper;

    /** 消息Mapper，用来操作 ai_message 表 */
    private final AiMessageMapper aiMessageMapper;

    /** stringRedisTemplate 的包装好的方法 */
    private final RedisService redisService;

    /** AI 流式任务共享线程池：有界队列，饱和时快速失败，由 Spring 统一管理生命周期 */
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    /**
     * 流式对话入口
     * 1. 获取用户id, 场景，和历史消息
     * 2. 创建会话或复用会话
     * 3. 将历史消息和 fastapi 返回的新消息组装一起
     * 4. 定义前端的emitter，用于拼接ai回答的answer，是否正常结束和是否存入数据库的两个参数
     * 5. 定义 Runnable，用户保存会话和消息
     * 6. 准备开始流式对话，先把会话id发给前端
     * 7. 启动异步任务，调用stream函数
     * 8. 完成后保存会话和消息
     */
    @Override
    public SseEmitter streamChat(AiChatRequest request) {
        // 获取用户id
        Long userId = SecurityUtils.requireUserId();

        // 本次调用的追踪标识，由入口过滤器生成，传给 FastAPI 后两侧日志可直接对齐
        String traceId = TraceContext.currentOrNew();

        // 从用户传过来的 request 中获取场景或对应消息
        String scene = resolveScene(request.getScene());

        // 将前端传过来的消息转换成统一格式
        List<AiChatMessage> messages = resolveMessages(request);

        // 判断是否是技能或 JD 拆解，技能或 JD 拆解生成失败不写入历史，改为流式成功后再落库；
        // 其他场景先存提问，避免上游失败时丢掉整次记录
        boolean saveOnSuccess = SCENE_SKILL.equals(scene) || SCENE_JD.equals(scene);

        // 创建会话或复用会话并保存
        AiConversation conversation = prepareConversation(
                userId,
                scene,
                request.getConversationId(),
                messages.get(0).getContent());

        // 如果不是技能或 JD 拆解，则先保存会话和消息，否则要等到流式成功后再保存
        if (!saveOnSuccess) {
            // 将会话id,user_id, scene 这些会话场景数据存进数据库
            persistConversation(conversation);

            // 将用户和会话消息通过会话id写入历史
            appendHistory(conversation.getId(), messages);
        }

        // 组装消息，将历史消息和额外消息（例如岗位详情，求职者信息等）组装成 AI 调用需要的格式
        List<Map<String, String>> payload = buildPayload(messages, request.getContext());

        // 创建 SseEmitter 并设置超时时间
        SseEmitter emitter = aiStreamClient.newEmitter();
        // 新建 StringBuilder 用于拼接 AI 回答内容
        StringBuilder answer = new StringBuilder();
        // 用于标记是否正常结束，初始为 false
        AtomicBoolean finishedNormally = new AtomicBoolean(false);
        // 用于标记是否已写入历史，初始为 false
        AtomicBoolean persisted = new AtomicBoolean(false);

        /**
         * 定义 Runnable，和局部函数有点像，但是它还能作为参数传给回调、线程池，在任何地方调用.run()
         * 保存 skill 和 jd 的消息
         */
        Runnable persistAnswer = () -> {
            // 如果是 skill 或 jd，并且流没有正常结束时，则先不运行后面的
            if (saveOnSuccess && !finishedNormally.get()) {
                return;
            }

            // 如果回答为空 或 如果已写入历史 ，则不运行后面的
            // （后面那段意思是如果persisted为false，则设置为true，否则不执行，再加个!则是取反）
            if (answer.isEmpty() || !persisted.compareAndSet(false, true)) {
                return;
            }

            // 如果是 skill 或 jd，并且流正常结束且回答不为空，或者不是 skill 或 jd
            // 且回答不为空，则保存会话和消息
            try {
                // 如果是 skill 或 jd，则先保存会话和消息，不是的之前保存过了
                if (saveOnSuccess) {
                    persistConversation(conversation);
                    appendHistory(conversation.getId(), messages);
                }

                // 保存ai消息到数据库
                saveMessage(conversation.getId(), nextSeq(conversation.getId()),
                        ROLE_ASSISTANT, answer.toString());

                // 更新会话最新时间
                touchConversation(conversation.getId());
            } catch (RuntimeException e) {
                log.error("保存 AI 回复失败: conversationId={}", conversation.getConversationId(), e);
            }
        };

        // 第一帧先推会话id给前端，供前端保存并沿用，避免首轮消息丢失
        aiStreamClient.sendData(emitter, Map.of("conversationId", conversation.getConversationId()));

        // 回调必须先注册：任务被拒绝或上游挂起时，仍需保证内容落库、连接可收尾
        emitter.onTimeout(TraceContext.wrap(traceId, persistAnswer));
        emitter.onError(e -> TraceContext.wrap(traceId, persistAnswer).run());

        // 启动异步任务，调用 FastAPI 流式接口（traceId 由线程池的 TaskDecorator 透传到子线程）
        try {
            aiStreamExecutor.execute(() -> {
                try {
                    // 调用 FastAPI 流式接口，发送请求并获取响应内容
                    aiStreamClient.stream(
                            userId,
                            conversation.getConversationId(),
                            traceId,
                            conversation.getScene(),
                            payload,
                            delta -> {
                                answer.append(delta);
                                aiStreamClient.sendData(emitter, Map.of("content", delta));
                            });

                    // 流式完成，标记正常结束
                    finishedNormally.set(true);

                    // 告诉前端流式完成
                    aiStreamClient.sendDone(emitter);

                } catch (Exception e) {
                    log.error("AI 流式对话失败: trace={}, conversationId={}",
                            traceId, conversation.getConversationId(), e);
                    // 流已开始，无法再改 HTTP 状态码，只能用事件告知
                    aiStreamClient.sendError(emitter, "AI 服务异常，请稍后重试");
                } finally {
                    // 全部完成，结束 sse 流
                    aiStreamClient.complete(emitter);

                    // 保存会话和消息
                    persistAnswer.run();
                }
            });
        } catch (TaskRejectedException rejected) {
            // 线程池饱和：响应头已发出，只能以 error 帧告知，并确保提问与连接都被收尾
            log.error("AI 流式任务被拒绝（线程池饱和）: trace={}, conversationId={}",
                    traceId, conversation.getConversationId());
            persistQuestionIfNeeded(conversation, messages, saveOnSuccess);
            aiStreamClient.sendError(emitter, BUSY_MESSAGE);
            aiStreamClient.complete(emitter);
        } catch (Exception exc) {
            log.error("AI 流式对话任务提交失败: trace={}, conversationId={}",
                    traceId, conversation.getConversationId(), exc);
            // 流已开始，无法再改 HTTP 状态码，只能用事件告知
            aiStreamClient.sendError(emitter, "AI 服务异常，请稍后重试");
            // 完成了，保存会话和消息
            aiStreamClient.complete(emitter);
        }

        return emitter;

    }

    /**
     * 任务被拒绝时的兜底落库：
     * 非 skill/jd 场景在提交任务前已保存提问，只有"成功后才落库"的场景需要在这里补上，
     * 否则用户会看到错误提示却在历史里找不到自己的提问。
     */
    private void persistQuestionIfNeeded(AiConversation conversation, List<AiChatMessage> messages,
                                         boolean saveOnSuccess) {
        if (!saveOnSuccess) {
            return;
        }
        try {
            persistConversation(conversation);
            appendHistory(conversation.getId(), messages);
        } catch (RuntimeException e) {
            log.error("保存用户提问失败: trace={}, conversationId={}",
                    TraceContext.current(), conversation.getConversationId(), e);
        }
    }

    /**
     * 解析场景：支持场景名与数字编码，非法场景按参数错误处理
     */
    private String resolveScene(String rawScene) {
        if (!StringUtils.hasText(rawScene)) {
            throw new BizException(ResultCode.BAD_REQUEST, "场景参数错误");
        }
        String text = rawScene.trim();
        String scene = SCENES.contains(text) ? text : SCENE_CODES.get(text);
        if (scene == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "场景参数错误");
        }
        return scene;
    }

    /**
     * 解析并清洗本轮消息：
     * 传了 messages 就按多轮处理且不再回退 input；否则用 input/content 组装单轮消息
     */
    private List<AiChatMessage> resolveMessages(AiChatRequest request) {

        // 如果是chat场景，则是message
        if (request.getMessages() != null) {
            return normalizeMessages(request.getMessages());
        }

        // 如果是skill 或 jd, 则是input
        String text = StringUtils.hasText(request.getInput())
                ? request.getInput().trim()
                : StringUtils.hasText(request.getContent()) ? request.getContent().trim() : "";
        if (text.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "输入参数错误");
        }
        return List.of(new AiChatMessage(ROLE_USER, text));
    }

    /**
     * 多轮消息校验：只允许 user / assistant，末条必须是 user，并限制条数与总长度
     */
    private List<AiChatMessage> normalizeMessages(List<AiChatMessage> rawMessages) {
        if (rawMessages.isEmpty() || rawMessages.size() > MAX_MESSAGES) {
            throw messagesError();
        }

        List<AiChatMessage> messages = new ArrayList<>(rawMessages.size());
        int total = 0;
        // 循环每条消息
        for (AiChatMessage item : rawMessages) {
            // 检查消息是否为空
            if (item == null) {
                throw messagesError();
            }

            // 检查角色是否为 user 或 assistant
            String role = item.getRole();
            if (!ROLE_USER.equals(role) && !ROLE_ASSISTANT.equals(role)) {
                throw messagesError();
            }
            // 检查内容是否为空或超长
            String content = item.getContent() == null ? "" : item.getContent().trim();
            if (content.isEmpty() || content.length() > MAX_CONTENT_LENGTH) {
                throw messagesError();
            }
            // 检查总长度是否超长
            total += content.length();
            if (total > MAX_TOTAL_LENGTH) {
                throw messagesError();
            }
            // 添加消息到列表
            messages.add(new AiChatMessage(role, content));
        }
        // 检查末条消息是否为 user
        if (!ROLE_USER.equals(messages.get(messages.size() - 1).getRole())) {
            throw messagesError();
        }
        return messages;
    }

    private BizException messagesError() {
        return new BizException(ResultCode.BAD_REQUEST, "messages 参数错误");
    }

    /**
     * 组装下发给模型的消息。
     * 传入历史消息和用户发的最新消息
     * 1. 判断是否有提示词
     * 2. 判断是否有补充消息
     * 3. 处理历史消息，将历史消息以 key: value 形式组成，再放进 payload
     * 4. 返回payload
     */
    private List<Map<String, String>> buildPayload(List<AiChatMessage> messages, String context) {
        List<Map<String, String>> payload = new ArrayList<>(messages.size() + 1);

        // 如果有提示词，将提示词放进payload
        if (StringUtils.hasText(aiProperties.getSystemPrompt())) {
            Map<String, String> system = new LinkedHashMap<>();
            system.put("role", ROLE_SYSTEM);
            system.put("content", aiProperties.getSystemPrompt());
            payload.add(system);
        }

        // 如果有补充信息（补充消息）
        String extra = context == null ? "" : context.trim();

        // 循环每条消息
        for (int i = 0; i < messages.size(); i++) {
            AiChatMessage message = messages.get(i);
            String content = message.getContent();

            // 如果是最后一条消息，将补充消息放进内容末尾
            boolean isLast = i == messages.size() - 1;
            if (isLast && !extra.isEmpty()) {
                content = content + "\n\n补充信息：\n" + extra;
            }

            // 每条消息都以key: value 形式组成，再放进payload
            Map<String, String> item = new LinkedHashMap<>();
            item.put("role", message.getRole());
            item.put("content", content);
            payload.add(item);
        }
        return payload;
    }

    /**
     * 解析或创建会话：
     * 已有会话直接复用（标题不再覆盖）；不存在时按客户端 ID 或新生成 ID 准备一条待落库记录。
     */
    private AiConversation prepareConversation(Long userId, String scene, String rawConversationId,
            String firstMessage) {
        // 校验并规范化会话 ID，判断是否规范并判断之前是否有会话 ID
        String conversationId = normalizeConversationId(rawConversationId);

        // 如果之前有会话 ID
        if (conversationId != null) {
            // 找到对应会话
            AiConversation existing = findByConversationId(userId, conversationId);

            // 有则直接返回
            if (existing != null) {
                return existing;
            }
        }

        // 无则创建新会话
        AiConversation created = new AiConversation();
        created.setConversationId(conversationId != null ? conversationId : generateConversationId());
        created.setUserId(userId);
        created.setScene(scene);
        created.setTitle(buildConversationTitle(firstMessage));
        LocalDateTime now = LocalDateTime.now();
        created.setCreateTime(now);
        created.setUpdateTime(now);
        return created;
    }

    /**
     * 会话落库：已存在则刷新场景与更新时间，不存在则插入。
     * 并发插入同一会话时回查已有记录，避免主键为空导致后续消息无法落库。
     */
    private void persistConversation(AiConversation conversation) {

        // 如果有id，说明是更新
        if (conversation.getId() != null) {
            aiConversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                    .eq(AiConversation::getId, conversation.getId())
                    .set(AiConversation::getScene, conversation.getScene())
                    .set(AiConversation::getUpdateTime, LocalDateTime.now()));
            return;
        }

        // 无则插入新会话
        try {
            aiConversationMapper.insert(conversation);
        } catch (DuplicateKeyException e) {
            // 出现主键冲突，说明有并发插入，回查已有记录
            AiConversation existing = findByConversationId(
                    conversation.getUserId(), conversation.getConversationId());
            if (existing == null) {
                throw e;
            }
            // 如果找到了，说明有并发插入，回查已有记录
            conversation.setId(existing.getId());
            conversation.setTitle(existing.getTitle());
        }
    }

    // 寻找第一个匹配的会话
    private AiConversation findByConversationId(Long userId, String conversationId) {
        return aiConversationMapper.selectOne(Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getUserId, userId)
                .last("LIMIT 1"));
    }

    /**
     * 校验并规范化会话 ID：只接受字母、数字、下划线与短横线
     */
    private String normalizeConversationId(String rawConversationId) {
        if (!StringUtils.hasText(rawConversationId)) {
            return null;
        }
        String text = rawConversationId.trim();
        return CONVERSATION_ID_PATTERN.matcher(text).matches() ? text : null;
    }

    private String generateConversationId() {
        return CONVERSATION_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, CONVERSATION_ID_LENGTH);
    }

    /**
     * 追加客户端提交的历史消息：
     * 客户端每轮都会带上完整历史，这里只补存尚未入库的尾部，避免重复插入
     * 
     * @param conversationPk 会话主键，也就是表中的 id
     * @param messages       历史消息
     */
    private void appendHistory(Long conversationPk, List<AiChatMessage> messages) {
        // 计算已存储的消息数量
        long stored = aiMessageMapper.selectCount(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationPk, conversationPk));
        
        // 如果数据库存的更多，说明有截断，先保存住最新的
        if (stored > messages.size()) {
            log.warn("历史条数不一致，可能前端截断: conversationPk={}, stored={}, size={}",
                    conversationPk, stored, messages.size());
            // 至少保住本轮提问
            AiChatMessage last = messages.get(messages.size() - 1);
            saveMessage(conversationPk, nextSeq(conversationPk), last.getRole(), last.getContent());
            return;
        }

        // 计算需要补存的消息数量，算出要从哪里开始补，并通过for循环补存
        int start = (int) stored;
        for (int i = start; i < messages.size(); i++) {
            AiChatMessage message = messages.get(i);
            saveMessage(conversationPk, nextSeq(conversationPk), message.getRole(), message.getContent());
        }
    }

    /**
     * 同一会话生成单调递增的序号seq
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

    /**
     * 保存消息
     */
    private void saveMessage(Long conversationPk, int seq, String role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationPk(conversationPk);
        message.setSeq(seq);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        aiMessageMapper.insert(message);
    }

    /**
     * 更新会话最新时间
     */
    private void touchConversation(Long conversationPk) {
        // update(null, wrapper) 不会触发自动填充，更新时间需显式赋值
        aiConversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                .eq(AiConversation::getId, conversationPk)
                .set(AiConversation::getUpdateTime, LocalDateTime.now())); // 必须要这一步，因为更新时没有传入实体，类里面的onUpdate是不会自动更新的
    }

    /**
     * 兜底标题：去掉代码块与 Markdown 标记，取首句并截断
     */
    private String buildConversationTitle(String content) {
        if (content == null) {
            return "";
        }
        String text = content
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("[#*`>]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.isEmpty()) {
            return "";
        }
        int sentenceEnd = -1;
        for (char mark : new char[] { '。', '！', '？', '!', '?', '；', ';' }) {
            int index = text.indexOf(mark);
            if (index > 0 && (sentenceEnd < 0 || index < sentenceEnd)) {
                sentenceEnd = index;
            }
        }
        String sentence = sentenceEnd > 0 ? text.substring(5, sentenceEnd) : text;
        return sentence.length() <= TITLE_MAX_LENGTH
                ? sentence
                : sentence.substring(0, TITLE_MAX_LENGTH) + "…";
    }
}