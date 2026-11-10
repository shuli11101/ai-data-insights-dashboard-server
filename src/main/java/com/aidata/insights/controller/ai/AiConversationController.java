package com.aidata.insights.controller.ai;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.service.ai.AiConversationService;
import com.aidata.insights.vo.ai.AiConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final AiConversationService aiConversationService;

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
}
