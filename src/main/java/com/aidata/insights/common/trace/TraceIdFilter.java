package com.aidata.insights.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪过滤器：为每个请求确定唯一 traceId，写入 MDC 并回填响应头
 *
 * <p>取值优先级：{@code X-Request-Id} → {@code X-Trace-Id} → 自动生成。
 * 入站值必须通过安全校验，否则丢弃重新生成，避免日志伪造。</p>
 *
 * <p>注册顺序设到最高，保证安全链与业务日志都能拿到 traceId。</p>
 *
 * <p>SSE 等异步请求结束后容器会做一次 ASYNC 派发，默认的 OncePerRequestFilter
 * 会跳过该次派发，导致异步阶段日志丢失 traceId；这里显式参与异步派发，
 * 并通过 request attribute 保证 traceId 不被重新生成。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 同一次请求内共享 traceId 的 attribute 键 */
    private static final String TRACE_ID_ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";

    /**
     * 显式参与异步派发，避免日志丢失 traceId
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 为请求确定 traceId 并写入 MDC
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 异步派发时复用首次请求算出的 traceId，否则重新协商
        String traceId = (String) request.getAttribute(TRACE_ID_ATTRIBUTE);
        boolean firstPass = traceId == null;
        if (firstPass) {
            traceId = TraceContext.resolve(request.getHeader(TraceContext.REQUEST_ID_HEADER));
            if (traceId == null) {
                traceId = TraceContext.resolve(request.getHeader(TraceContext.HEADER));
            }
            if (traceId == null) {
                traceId = TraceContext.generate();
            }
            request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        }

        // 写入当前线程 MDC，供日志模板中的 %X{traceId} 使用
        TraceContext.set(traceId);

        // 响应头只在首次派发且未提交时写入，流已开始后无法再改头
        if (firstPass && !response.isCommitted()) {
            response.setHeader(TraceContext.HEADER, traceId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程会回到 Tomcat 线程池，必须清理避免污染后续请求
            TraceContext.clear();
        }
    }
}