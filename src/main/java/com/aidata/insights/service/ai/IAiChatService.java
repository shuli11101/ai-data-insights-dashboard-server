package com.aidata.insights.service.ai;

import com.aidata.insights.dto.ai.AiChatRequest;
import com.aidata.insights.dto.ai.AiReportRequest;
import com.aidata.insights.vo.ai.AiReportVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务：Java 做网关，FastAPI 只负责推理
 */
public interface IAiChatService {

    /**
     * 流式对话：转发 AI 服务的 SSE，同时把用户与助手消息落库
     */
    SseEmitter streamChat(AiChatRequest request);

    /**
     * 同步长请求（报告生成、简历解析等）
     */
    AiReportVO generateReport(AiReportRequest request);
}