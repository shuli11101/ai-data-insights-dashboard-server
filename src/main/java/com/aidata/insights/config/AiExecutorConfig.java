package com.aidata.insights.config;

import com.aidata.insights.common.trace.TraceContext;
import com.aidata.insights.config.properties.AiExecutorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 流式任务线程池配置
 *
 * <p>为什么必须有界：AI 流式请求是长耗时阻塞任务（单次可达数十秒到数分钟），
 * 使用 {@code Executors.newCachedThreadPool()} 时线程数会随并发线性增长直至 OOM。
 * 这里用固定上限 + 有界队列，队列满后直接拒绝，由业务侧返回友好的 error 帧。</p>
 *
 * <p>为什么用 AbortPolicy：{@code CallerRunsPolicy} 会让 Tomcat 线程去跑长任务，
 * 反而拖垮 Web 容器，比拒绝更糟。</p>
 *
 * <p>traceId：通过 TaskDecorator 在提交时捕获当前线程的 traceId，
 * 执行线程内写入 MDC 并在结束后还原，避免线程复用造成日志串号。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiExecutorConfig {

    /** 线程池 Bean 名称，业务侧按名称注入 */
    public static final String AI_STREAM_EXECUTOR = "aiStreamExecutor";

    private final AiExecutorProperties properties;

    /**
     * AI 流式任务共享线程池：对话与匹配共用，避免重复创建与配置分散
     */
    @Bean(AI_STREAM_EXECUTOR)
    public ThreadPoolTaskExecutor aiStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCoreSize());
        executor.setMaxPoolSize(properties.getMaxSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        // 饱和时快速失败，抛出 TaskRejectedException
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 提交时捕获 traceId（decorate 在提交线程执行），执行线程内写入 MDC
        executor.setTaskDecorator(task -> TraceContext.wrap(TraceContext.current(), task));
        // 流式任务可能长达 stream-timeout，停机时不无限等待，直接中断
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());

        log.info("AI 流式线程池初始化: core={}, max={}, queue={}, prefix={}, awaitTermination={}s",
                properties.getCoreSize(), properties.getMaxSize(), properties.getQueueCapacity(),
                properties.getThreadNamePrefix(), properties.getAwaitTerminationSeconds());
        return executor;
    }
}