package com.aidata.insights.controller.user;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.service.user.MatchReportService;
import com.aidata.insights.vo.user.MatchReportListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位匹配分析报告接口
 *
 * <p>需要登录（JWT），只返回当前用户自己保存的报告。</p>
 */
@Tag(name = "03. 用户")
@RestController
@RequestMapping("/user/match-report")
@RequiredArgsConstructor
public class MatchReportController {

    private final MatchReportService matchReportService;

    /**
     * 获取当前用户已保存的全部分析报告
     */
    @Operation(summary = "分析报告列表：按最后更新时间倒序")
    @GetMapping("/list")
    public Result<MatchReportListVO> list() {
        return Result.ok(matchReportService.listReports());
    }
}
