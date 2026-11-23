package com.aidata.insights.common.trace;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 链路追踪上下文：统一管理 traceId 的生成、入站复用与 MDC 读写
 *
 * <p>格式与 FastAPI 侧保持一致：32 位小写十六进制（UUID 去掉短横线），
 * 两边用同一个值才能把 Java 与 AI 服务的日志串起来。</p>
 *
 * <p>注意：MDC 基于 ThreadLocal，自建线程池中的异步任务不会自动继承父线程的 MDC，
 * 必须显式把 traceId 传入子线程，可用 {@link #wrap(String, Runnable)} 包装任务。</p>
 */
public final class TraceContext {

    /** 对外回传的追踪响应头 */
    public static final String HEADER = "X-Trace-Id";

    /** 上游/网关可能已经注入的请求 ID 头，有值则优先复用 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** MDC 中的键名，需与 logback 的 %X{traceId} 保持一致 */
    public static final String TRACE_ID = "traceId";

    /** 入站头的最大允许长度，防止超长头污染日志 */
    private static final int MAX_LENGTH = 64;

    /** 只接受字母、数字、下划线与短横线，避免换行注入等日志伪造 */
    private static final Pattern SAFE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1," + MAX_LENGTH + "}$");

    private TraceContext() {
    }

    /**
     * 生成新的 traceId：32 位十六进制，与 FastAPI 的 uuid4().hex 同格式
     */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 校验并复用入站 traceId，非法或为空时返回 null 交由调用方生成
     */
    public static String resolve(String inbound) {
        if (inbound == null) {
            return null;
        }
        String text = inbound.trim();
        return SAFE_PATTERN.matcher(text).matches() ? text : null;
    }

    /**
     * 读取当前线程的 traceId，可能为 null（如未经过过滤器或异步线程）
     */
    public static String current() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 读取当前线程的 traceId，没有则临时生成一个（仅用于兜底，不写入 MDC）
     */
    public static String currentOrNew() {
        String traceId = MDC.get(TRACE_ID);
        return traceId != null ? traceId : generate();
    }

    /**
     * 写入当前线程的 MDC
     */
    public static void set(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * 清理当前线程的 MDC，必须在线程返回线程池前调用，避免污染后续任务
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
    }

    /**
     * 把任务包装成带 traceId 上下文的版本：
     * 执行前写入 MDC，结束后还原为原值，用于自建线程池中的异步任务。
     */
    public static Runnable wrap(String traceId, Runnable task) {
        return () -> {
            String previous = MDC.get(TRACE_ID);
            set(traceId);
            try {
                task.run();
            } finally {
                if (previous == null) {
                    clear();
                } else {
                    set(previous);
                }
            }
        };
    }
}