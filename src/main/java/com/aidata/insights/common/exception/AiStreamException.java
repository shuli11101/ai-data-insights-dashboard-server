package com.aidata.insights.common.exception;

import lombok.Getter;

/**
 * AI 上游流内错误：FastAPI 已在 SSE 中以 error 帧告知本次调用失败
 *
 * <p>与 {@link BizException} 的区别：本异常承载上游返回的 traceId，
 * 便于把 Java 日志与 AI 服务日志直接对齐，是排查流式故障的主要线索。</p>
 *
 * <p>注意：此时 HTTP 响应头早已发出，无法再改状态码，
 * 只能由业务侧转成 {@code {"error":"..."}} 数据帧告知前端。</p>
 */
@Getter
public class AiStreamException extends RuntimeException {

    /** 上游（FastAPI）返回的 traceId，可能为空 */
    private final String upstreamTraceId;

    public AiStreamException(String message, String upstreamTraceId) {
        super(message);
        this.upstreamTraceId = upstreamTraceId;
    }
}