package com.aidata.insights.controller.ai;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.ai.DeleteConversationRequest;
import com.aidata.insights.service.ai.IAiConversationService;
import com.aidata.insights.vo.ai.AiConversationDetailVO;
import com.aidata.insights.vo.ai.AiConversationVO;
import com.aidata.insights.vo.ai.DeleteConversationResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 会话接口
 *
 * <p>需要登录（JWT），只返回当前用户自己的会话。</p>
 */
@Tag(name = "04. AI")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiConversationController {

    private final IAiConversationService aiConversationService;

    /**
     * 获取会话列表
     */
    @Operation(summary = "会话列表：按最后更新时间倒序")
    @GetMapping("/conversations")
    public Result<List<AiConversationVO>> conversations(
            @Parameter(description = "返回条数上限，默认 50，最大 200", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(aiConversationService.listConversations(limit));
    }

    /**
     * 会话详情
     */
    @Operation(summary = "会话详情：按 conversationId 返回全部消息")
    @GetMapping("/conversation")
    public Result<AiConversationDetailVO> conversation(
            @Parameter(description = "会话 ID", example = "c-mw6fs2hoea60467f")
            @RequestParam(required = false) String conversationId) {
        return Result.ok(aiConversationService.getConversation(conversationId));
    }

    /**
     * 批量删除会话及其全部消息
     */
    @Operation(summary = "批量删除会话：单次 1-50 个，返回 deleted / missing")
    @DeleteMapping("/conversation")
    public Result<DeleteConversationResultVO> deleteConversation(
            @Valid @RequestBody DeleteConversationRequest request) {
        return Result.ok(aiConversationService.deleteConversations(request.getConversationIds()));
    }
}
