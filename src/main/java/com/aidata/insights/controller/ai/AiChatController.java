package com.aidata.insights.controller.ai;

import com.aidata.insights.dto.ai.AiChatRequest;
import com.aidata.insights.dto.ai.AiMatchRequest;
import com.aidata.insights.service.ai.IAiChatService;
import com.aidata.insights.service.ai.IAiMatchService;
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
 * <p>流式接口返回 text/event-stream，仅用 data 行承载内容：
 * 首帧 {"conversationId":"c-xxx"}、增量 {"content":"..."}、结束 [DONE]、异常 {"error":"..."}。</p>
 */
@Tag(name = "04. AI")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final IAiChatService aiChatService;

    private final IAiMatchService aiMatchService;

    /**
     * 流式对话
     */
    @Operation(summary = "流式对话：data 行依次下发 conversationId / content / [DONE] / error")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.streamChat(request);
    }

    /**
     * 岗位匹配度分析
     */
    @Operation(summary = "岗位匹配度分析：data 行下发 cached / content / [DONE] / error")
    @PostMapping(value = "/match", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter match(@Valid @RequestBody AiMatchRequest request) {
        return aiMatchService.streamMatch(request);
    }
}