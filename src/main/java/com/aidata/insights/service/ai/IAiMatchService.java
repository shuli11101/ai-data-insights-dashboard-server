package com.aidata.insights.service.ai;

import com.aidata.insights.dto.ai.AiMatchRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface IAiMatchService {

    /**
     * 岗位匹配度分析：命中已保存报告直接下发，否则流式生成
     */
    SseEmitter streamMatch(AiMatchRequest request);
}