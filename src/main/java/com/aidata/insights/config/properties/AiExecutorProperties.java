package com.aidata.insights.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AI 流式任务线程池配置项
 *
 * <p>AI 流式请求属于长耗时阻塞任务，必须使用有界线程池：
 * 队列满后快速失败，由接口层返回"使用人数较多"的提示，而不是无限新建线程。</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.ai.executor")
public class AiExecutorProperties {

    /**
     * 常驻线程数
     */
    @Min(1)
    private int coreSize = 8;

    /**
     * 峰值线程上限，达到后任务进入队列
     */
    @Min(1)
    private int maxSize = 32;

    /**
     * 排队上限，队列也满时拒绝任务
     */
    @Min(0)
    private int queueCapacity = 100;

    /**
     * 线程名前缀，便于在日志中区分流式任务线程
     */
    private String threadNamePrefix = "ai-stream-";

    /**
     * 停机时等待任务响应中断的秒数，应与 server.shutdown 的等待时间匹配
     */
    @Min(0)
    private int awaitTerminationSeconds = 20;
}