package com.aidata.insights.service.ai;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.AiStreamException;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.trace.TraceContext;
import com.aidata.insights.config.properties.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 流式客户端：唯一与 FastAPI 通信的地方
 *
 * <p>职责：调用 {@code /internal/chat/stream}，把上游 {@code data:{"text"}} 解析成增量回调，
 * 并按前端契约写 SSE 帧（conversationId / content / [DONE] / error）。</p>
 *
 * <p>业务侧（对话、匹配）只关心"拿到增量文本"和"往流里写东西"，
 * 不再各自维护 HttpClient 与帧格式。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStreamClient {

    /** SSE 流式接口正常结束标记 */
    private static final String DONE_FRAME = "[DONE]";

    /** AI 服务配置 */
    private final AiProperties aiProperties;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /** HTTP 客户端 */
    private HttpClient httpClient;

    /** 初始化 HTTP 客户端与日志 */
    @PostConstruct
    void init() {
        // 创建 HTTP 客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(aiProperties.getConnectTimeout()) // 传入ai配置的连接超时时间
                .version(HttpClient.Version.HTTP_1_1) // 显式指定 HTTP/1.1，避免升级请求问题
                .build();

        // 启动时确认配置：令牌只打长度，不打印明文
        log.info("AI 网关配置: baseUrl={}, internalToken 长度={}, streamTimeout={}",
                aiProperties.getBaseUrl(),  // AI 服务基础 URL
                aiProperties.getInternalToken().length(),  // AI 服务内部令牌长度
                aiProperties.getStreamTimeout());  // AI 服务流式接口超时时间
    }

    /**
     * 流式响应专用的 emitter，用于将 fastapi 的结果转为 sse 流式推送给前端
     * @return SseEmitter
     */
    public SseEmitter newEmitter() {
        return new SseEmitter(aiProperties.getStreamTimeout().toMillis());
    }

    /**
     * 调用 FastAPI 流式接口，发送请求并获取响应内容，再推给前端；非 200 直接抛业务异常
     * 
     * 1. 存请求参数
     * 2. 创建 http 请求，并将请求参数(json版)放进去
     * 3. 使用 httpclient 发送 http 请求并获取响应存进 response
     * 4. 将响应回来的数据（响应体）进行逐行拿出处理
     * 5. 通过 onDelta 回调给业务侧
     *
     * @param conversationId 业务会话标识，随请求体透传给 FastAPI，不再充当追踪标识
     * @param traceId        本次调用的追踪标识，与本地日志的 traceId 一致，用于两侧日志对齐
     */
    public void stream(Long userId, String conversationId, String traceId, String scene,
                       List<Map<String, String>> messages,
                       Consumer<String> onDelta) throws Exception {
        // 创建一个 hashMap 来存储请求参数
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("conversationId", conversationId);
        payload.put("scene", scene);
        payload.put("messages", messages);

        // 将请求参数转换为 JSON 字符串
        String body = objectMapper.writeValueAsString(payload);

        log.debug("调用 AI 流式接口: trace={}, conversationId={}, scene={}, messages={}",
                traceId, conversationId, scene, messages.size());

        /**
         * 创建 HTTP 请求
         * 1. 发给谁
         * 2. 等多久
         * 3. 用什么头
         * 4. 用什么请求体发
         * 5. 造出来
         */
        HttpRequest httpRequest = HttpRequest.newBuilder(
                        URI.create(aiProperties.getBaseUrl() + "/internal/chat/stream")) // AI 服务流式接口地址
                .timeout(aiProperties.getStreamTimeout()) // 传入ai配置的流式接口超时时间
                .header("Content-Type", "application/json") // 请求内容类型
                .header("X-Internal-Token", aiProperties.getInternalToken()) // AI 服务内部令牌
                // 传本次调用独有的 traceId，FastAPI 会原样使用并回填，两侧日志才能一一对应
                .header(TraceContext.HEADER, traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)) // 请求体
                .build();

        /**
         * 发送 HTTP 请求并获取响应
         * HttpResponse.BodyHandlers.ofInputStream()：不把响应体（fastapi输出的内容）一次性存进内存，
         * 而是返回一个 InputStream，让调用方法边读边处理
         */
        HttpResponse<InputStream> response = httpClient.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());
            
        // 检查响应状态码，非 200 抛异常
        if (response.statusCode() != 200) {
            // 上游正常响应会在头里回填 traceId；错误响应可能丢失该头，此时以本地 traceId 为准
            String upstreamTraceId = response.headers().firstValue(TraceContext.HEADER).orElse(null);
            // 详情只进日志，不返回给前端
            log.error("AI 服务返回异常状态: trace={}, upstreamTrace={}, status={}, body={}",
                    traceId, upstreamTraceId, response.statusCode(), readAllQuietly(response.body()));
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 服务不可用，请稍后重试");
        }

        /**
         * 读取响应体并处理
         * BufferedReader：逐行读取响应体
         * InputStreamReader：将 InputStream 转换为 Reader
         * StandardCharsets.UTF_8：指定字符编码为 UTF-8
         */
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            // 逐行读取
            while ((line = reader.readLine()) != null) {
                // 只处理 data 行
                if (!line.startsWith("data:")) {
                    continue;
                }
                // 去掉 data: 前缀并.trim()
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }

                // 解析 JSON，转为能通过字段取值，而不是字符串
                JsonNode node = objectMapper.readTree(data);

                // 增量正文：正常情况，回调给业务侧后继续读下一行
                if (node.hasNonNull("text")) {
                    // 调用 onDelta 回调，将 text 字段的值传给业务侧
                    onDelta.accept(node.get("text").asText());
                    continue;
                }

                // 错误帧：上游已判定本次调用失败，必须中断，不能当作空内容静默跳过
                String error = firstText(node, "error", "message");
                if (error != null) {
                    // 上游 error 帧可能带 traceId（驼峰或下划线两种写法都要兼容）
                    String upstreamTraceId = firstText(node, "traceId", "trace_id");
                    log.error("AI 上游返回 error 帧: trace={}, upstreamTrace={}, error={}",
                            traceId, upstreamTraceId, error);
                    throw new AiStreamException(error, upstreamTraceId);
                }
            }
        }
    }

    /**
     * 将不同类型数据发送给前端 SSE
     */
    public void sendData(SseEmitter emitter, Object data) {
        try {
            if (data instanceof String text) {
                emitter.send(SseEmitter.event().data(text, MediaType.TEXT_PLAIN));
            } else {
                emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
            }
        } catch (Exception e) {
            // 客户端已断开属正常场景，这里只降级记录
            log.debug("SSE 推送失败（客户端可能已断开）");
        }
    }

    /**
     * 正常结束标记，告诉前端响应结束
     */
    public void sendDone(SseEmitter emitter) {
        sendData(emitter, DONE_FRAME);
    }

    /**
     * 流内异常标记：此时响应头已发出，无法再改状态码，只能通过数据帧表示错误
     */
    public void sendError(SseEmitter emitter, String message) {
        sendData(emitter, Map.of("error", message));
    }

    /**
     * 结束 SSE，客户端已断开时静默忽略
     */
    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (RuntimeException e) {
            log.debug("SSE 连接已关闭");
        }
    }

    /**
     * 按顺序取第一个非空文本字段，兼容上游字段命名差异
     */
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    /**
     * 静默读取输入流所有内容，出错时返回空字符串
     */
    private String readAllQuietly(InputStream in) {
        try (InputStream stream = in) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}