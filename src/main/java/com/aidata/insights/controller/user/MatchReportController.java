package com.aidata.insights.controller.user;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.dto.user.SaveMatchReportRequest;
import com.aidata.insights.service.user.IMatchReportService;
import com.aidata.insights.vo.user.MatchReportListVO;
import com.aidata.insights.vo.user.MatchReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final IMatchReportService matchReportService;

   

    /**
     * 获取当前用户已保存的全部分析报告
     */
    @Operation(summary = "分析报告列表：按最后更新时间倒序")
    @GetMapping("/list")
    public Result<MatchReportListVO> list() {
        return Result.ok(matchReportService.listReports());
    }

    /**
     * 保存岗位匹配报告
     */
    @Operation(summary = "保存岗位匹配报告")
    @PostMapping
    public Result<MatchReportVO> save(@Valid @RequestBody SaveMatchReportRequest request) {
        return Result.ok(matchReportService.saveReport(request));
    }

    /**
     * 获取当前用户已保存的岗位匹配报告
     */
    @Operation(summary = "获取当前用户已保存的岗位匹配报告")
    @GetMapping
    public Result<MatchReportVO> get(@RequestParam Long jobId) {
        return Result.ok(matchReportService.getReport(jobId));
    }

    /**
     * 删除当前用户已保存的岗位匹配报告
     */
    @Operation(summary = "删除当前用户已保存的岗位匹配报告")
    @DeleteMapping
    public Result<?> delete(@RequestParam Long jobId) {
        boolean isSuccess = matchReportService.deleteReport(jobId);
        if (!isSuccess) {
            return Result.fail(ResultCode.NOT_FOUND, "报告不存在");
        }
        return Result.ok(isSuccess);
    }
}
