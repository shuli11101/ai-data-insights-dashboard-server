package com.aidata.insights.service.user;

import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.entity.MatchReport;
import com.aidata.insights.mapper.home.JobPositionMapper;
import com.aidata.insights.mapper.user.MatchReportMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.vo.user.MatchReportListVO;
import com.aidata.insights.vo.user.MatchReportVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 岗位匹配分析报告服务
 *
 * <p>报告正文是 Markdown，技能覆盖率、薪资偏差等指标不单独成列，需要从正文中解析；
 * 岗位名称、公司、地点、经验要求则取自 job_positions。</p>
 */
@Service
@RequiredArgsConstructor
public class MatchReportService {

    /** 正文中的「技能覆盖率：0」 */
    private static final Pattern SKILL_COVERAGE_PATTERN = Pattern.compile("技能覆盖率[：:]\\s*(\\d+)");

    /** 正文中的「偏差百分比：-20」；「未提及」时不匹配，返回 null */
    private static final Pattern SALARY_DEVIATION_PATTERN = Pattern.compile("偏差百分比[：:]\\s*(-?\\d+)");

    private final MatchReportMapper matchReportMapper;

    private final JobPositionMapper jobPositionMapper;

    /**
     * 当前用户已保存的全部分析报告，按最后更新时间倒序
     */
    public MatchReportListVO listReports() {
        Long userId = SecurityUtils.requireUserId();

        List<MatchReport> reports = matchReportMapper.selectList(Wrappers.<MatchReport>lambdaQuery()
                .eq(MatchReport::getUserId, userId)
                .orderByDesc(MatchReport::getUpdateTime)
                .orderByDesc(MatchReport::getId));
        if (reports.isEmpty()) {
            return new MatchReportListVO(List.of(), 0);
        }

        List<Long> jobIds = reports.stream()
                .map(MatchReport::getJobId)
                .distinct()
                .toList();
        Map<Long, JobPosition> jobs = jobPositionMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(JobPosition::getId, job -> job));

        List<MatchReportVO> list = reports.stream()
                .map(report -> toVO(report, jobs.get(report.getJobId())))
                .toList();
        return new MatchReportListVO(list, reports.size());
    }

    private MatchReportVO toVO(MatchReport report, JobPosition job) {
        MatchReportVO vo = new MatchReportVO();
        vo.setReportId(report.getId());
        vo.setJobId(report.getJobId());
        vo.setScore(report.getScore());
        vo.setSaveTime(report.getCreateTime());
        vo.setContent(report.getContent());
        vo.setSkillCoverage(extractInt(SKILL_COVERAGE_PATTERN, report.getContent()));
        vo.setSalaryDeviation(extractInt(SALARY_DEVIATION_PATTERN, report.getContent()));

        // 岗位被删除时仍返回报告，只是岗位信息为空
        if (job != null) {
            vo.setJobName(job.getJobName());
            vo.setCompany(job.getCompany());
            vo.setExperience(job.getExperience());
            vo.setLocation(buildLocation(job));
        }
        return vo;
    }

    private Integer extractInt(Pattern pattern, String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /**
     * 工作地点展示为「城市·区县」，缺区县时只返回城市
     */
    private String buildLocation(JobPosition job) {
        if (!StringUtils.hasText(job.getCity())) {
            return job.getDistrict();
        }
        return StringUtils.hasText(job.getDistrict())
                ? job.getCity() + "·" + job.getDistrict()
                : job.getCity();
    }
}
