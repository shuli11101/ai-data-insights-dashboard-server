package com.aidata.insights.controller.user;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.dto.user.DeleteMatchReportRequest;
import com.aidata.insights.dto.user.SaveMatchReportRequest;
import com.aidata.insights.service.user.IMatchReportService;
import com.aidata.insights.vo.user.MatchReportDeleteVO;
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

import java.util.ArrayList;
import java.util.List;


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

    /** 一次批量删除最多 50 个，避免超长 SQL */
    private static final int MAX_BATCH_DELETE = 50;

   

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
        MatchReportVO report = matchReportService.getReport(jobId);
        if (report == null) {
            return Result.fail(ResultCode.NOT_FOUND, "暂无匹配报告");
        }
        return Result.ok(report);
    }

    /**
     * 批量删除分析报告：单个删除也走这个接口
     *
     * <p>ID 可从 body.jobIds、query.jobIds（逗号分隔）或 query.jobId 传入；
     * 只删当前用户自己的报告，不属于自己的一律计入 missing。</p>
     */
    @Operation(summary = "批量删除分析报告：返回 deleted 与 missing")
    @DeleteMapping
    public Result<MatchReportDeleteVO> delete(
            @RequestBody(required = false) DeleteMatchReportRequest body,
            @RequestParam(required = false) List<Long> jobIds,
            @RequestParam(required = false) Long jobId) {
        List<Long> ids = mergeJobIds(body, jobIds, jobId);
        if (ids.isEmpty() || ids.size() > MAX_BATCH_DELETE) {
            return Result.fail(ResultCode.BAD_REQUEST,
                    "jobIds 参数错误（1-" + MAX_BATCH_DELETE + " 个合法 ID）");
        }

        MatchReportDeleteVO result = matchReportService.deleteReports(ids);
        if (result.getDeleted() == 0) {
            return Result.fail(ResultCode.NOT_FOUND, "报告不存在");
        }
        return Result.ok(result);
    }

    /**
     * 合并三种传参方式，过滤非法 ID 并去重
     */
    private List<Long> mergeJobIds(DeleteMatchReportRequest body, List<Long> queryIds, Long singleId) {
        List<Long> merged = new ArrayList<>();
        if (body != null && body.getJobIds() != null) {
            merged.addAll(body.getJobIds());
        }
        if (queryIds != null) {
            merged.addAll(queryIds);
        }
        if (singleId != null) {
            merged.add(singleId);
        }
        return merged.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }
}
