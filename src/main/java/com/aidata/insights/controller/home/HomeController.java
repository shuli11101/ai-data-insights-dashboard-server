package com.aidata.insights.controller.home;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.home.JobQuery;
import com.aidata.insights.service.home.IJobPositionService;
import com.aidata.insights.vo.home.AreaJobCountVO;
import com.aidata.insights.vo.home.CityOptionVO;
import com.aidata.insights.vo.home.ExperienceSalaryVO;
import com.aidata.insights.vo.home.JobDetailVO;
import com.aidata.insights.vo.home.JobListVO;
import com.aidata.insights.vo.home.SalaryRangeVO;
import com.aidata.insights.vo.home.SkillTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页接口：岗位列表 / 详情 + 首页图表统计
 *
 * <p>路径均在 /api/home 下，且已在 SecurityConfig 白名单中免登录。</p>
 */
@Tag(name = "02. 首页")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final IJobPositionService jobPositionService;

    /**
     * 获取职位列表
     */
    @Operation(summary = "岗位列表：多条件分页查询")
    @GetMapping("/jobList")
    public Result<JobListVO> jobList(JobQuery query) {
        return Result.ok(jobPositionService.pageJobs(query));
    }

    /**
     * 获取职位详情
     */
    @Operation(summary = "岗位详情：优先读 Redis 缓存")
    @GetMapping("/jobDetail")
    public Result<JobDetailVO> jobDetail(
            @Parameter(description = "岗位 ID", required = true) String id) {
        return Result.ok(jobPositionService.getJobDetail(id));
    }

    /**
     * 按薪资区间统计岗位数量
     */
    @Operation(summary = "薪资区间岗位数量分布")
    @GetMapping("/salaryRange")
    public Result<List<SalaryRangeVO>> salaryRange() {
        return Result.ok(jobPositionService.salaryRange());
    }

    /**
     * 技术栈热度 Top 10
     */
    @Operation(summary = "技术栈热度 Top 10")
    @GetMapping("/skillTags")
    public Result<List<SkillTagVO>> skillTags() {
        return Result.ok(jobPositionService.hotSkillTags());
    }

    /**
     * 城市岗位数量（前 8 + 其他）
     */
    @Operation(summary = "城市岗位数量分布（前 8 + 其他）")
    @GetMapping("/district")
    public Result<List<AreaJobCountVO>> district() {
        return Result.ok(jobPositionService.districtJobCounts());
    }

    /**
     * 按工作经验分档统计平均薪资
     */
    @Operation(summary = "工作经验与平均薪资")
    @GetMapping("/experienceSalary")
    public Result<List<ExperienceSalaryVO>> experienceSalary() {
        return Result.ok(jobPositionService.experienceSalary());
    }

    /**
     * 城市下拉选项
     */
    @Operation(summary = "城市下拉选项")
    @GetMapping("/city")
    public Result<List<CityOptionVO>> city() {
        return Result.ok(jobPositionService.cityOptions());
    }
}
