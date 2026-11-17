package com.aidata.insights.controller.ai;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.ai.AiChatRequest;
import com.aidata.insights.dto.ai.AiReportRequest;
import com.aidata.insights.service.ai.IAiChatService;
import com.aidata.insights.vo.ai.AiReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话接口（需要登录）
 *
 * <p>流式接口返回 text/event-stream，事件为 delta / done / error；
 * 同步接口返回 JSON，用于报告类长请求。</p>
 */
@Tag(name = "04. AI")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final IAiChatService aiChatService;

    /**
     * 流式对话
     */
    @Operation(summary = "流式对话：SSE 返回 delta / done / error")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.streamChat(request);
    }

    /**
     * 同步长请求
     */
    @Operation(summary = "同步长请求：报告生成、简历解析等")
    @PostMapping("/report")
    public Result<AiReportVO> report(@Valid @RequestBody AiReportRequest request) {
        return Result.ok(aiChatService.generateReport(request), "生成成功");
    }
}