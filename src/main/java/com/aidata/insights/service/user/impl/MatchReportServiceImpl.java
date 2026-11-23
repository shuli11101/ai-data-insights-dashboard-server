package com.aidata.insights.service.user.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.dto.user.SaveMatchReportRequest;
import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.entity.MatchReport;
import com.aidata.insights.mapper.home.JobPositionMapper;
import com.aidata.insights.mapper.user.MatchReportMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.user.IMatchReportService;
import com.aidata.insights.vo.user.MatchReportDeleteVO;
import com.aidata.insights.vo.user.MatchReportListVO;
import com.aidata.insights.vo.user.MatchReportVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class MatchReportServiceImpl implements IMatchReportService {

    /** 正文中的「技能覆盖率：0」 */
    private static final Pattern SKILL_COVERAGE_PATTERN = Pattern.compile("技能覆盖率[：:]\\s*(\\d+)");

    /** 正文中的「偏差百分比：-20」；「未提及」时不匹配，返回 null */
    private static final Pattern SALARY_DEVIATION_PATTERN = Pattern.compile("偏差百分比[：:]\\s*(-?\\d+)");

    /** 正文中的「## 综合匹配分\n72」 */
    private static final Pattern MATCH_SCORE_PATTERN = Pattern.compile("##+\\s*综合匹配分\\s*[:：]?\\s*\\n*\\s*(\\d{1,3})");

    private final MatchReportMapper matchReportMapper;

    private final JobPositionMapper jobPositionMapper;

    /**
     * 当前用户已保存的全部分析报告，按最后更新时间倒序
     */
    @Override
    public MatchReportListVO listReports() {
        // 获取当前用户id
        Long userId = SecurityUtils.requireUserId();

        // 根据用户id查询匹配报告
        List<MatchReport> reports = matchReportMapper.selectList(Wrappers.<MatchReport>lambdaQuery()
                .eq(MatchReport::getUserId, userId)
                .orderByDesc(MatchReport::getUpdateTime)
                .orderByDesc(MatchReport::getId));
        if (reports.isEmpty()) {
            return new MatchReportListVO(List.of(), 0);
        }

        // 根据报告中的岗位id查询岗位信息
        List<Long> jobIds = reports.stream()
                .map(MatchReport::getJobId)
                .distinct()
                .toList();
        Map<Long, JobPosition> jobs = jobPositionMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(JobPosition::getId, job -> job));

        // 将报告和岗位信息组合成VO
        List<MatchReportVO> list = reports.stream()
                .map(report -> toVO(report, jobs.get(report.getJobId())))
                .toList();
        return new MatchReportListVO(list, reports.size());
    }

    /**
     * 保存分析报告
     */
    @Override
    public MatchReportVO saveReport(SaveMatchReportRequest request) {

        Long userId = SecurityUtils.requireUserId();

        JobPosition job = jobPositionMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BizException(ResultCode.NOT_FOUND, "岗位不存在");
        }

        // 前端只提交正文，分数由服务端从「## 综合匹配分」解析，客户端传的 score 一律忽略
        int score = parseMatchScore(request.getContent());

        // 同一用户同一岗位只保留一份报告（uk_user_job），重复保存时覆盖旧内容
        MatchReport report = findByUserAndJob(userId, request.getJobId());
        if (report == null) {
            report = new MatchReport(userId, request.getJobId(), score, request.getContent());
            try {
                matchReportMapper.insert(report);
            } catch (DuplicateKeyException e) {
                // 并发提交时另一个请求已插入，转为覆盖更新
                report = findByUserAndJob(userId, request.getJobId());
                if (report == null) {
                    throw new BizException(ResultCode.INTERNAL_ERROR, "报告保存失败，请重试");
                }
                updateContent(report, score, request.getContent());
            }
        } else {
            updateContent(report, score, request.getContent());
        }

        return toVO(report, job);
    }

    /**
     * 获取当前用户已保存的分析报告
     */
    @Override
    public MatchReportVO getReport(Long jobId) {
        MatchReport report = findByUserAndJob(SecurityUtils.requireUserId(), jobId);
        if (report == null) {
            return null;
        }
        return toVO(report, jobPositionMapper.selectById(jobId));
    }

    /**
     * 批量删除当前用户已保存的分析报告：只删属于自己的记录，不属于自己的计入 missing
     */
    @Override
    public MatchReportDeleteVO deleteReports(List<Long> jobIds) {
        Long userId = SecurityUtils.requireUserId();
        List<Long> ids = jobIds.stream().filter(Objects::nonNull).distinct().toList();

        List<MatchReport> found = matchReportMapper.selectList(Wrappers.<MatchReport>lambdaQuery()
                .eq(MatchReport::getUserId, userId)
                .in(MatchReport::getJobId, ids));
        if (!found.isEmpty()) {
            matchReportMapper.deleteBatchIds(found.stream().map(MatchReport::getId).toList());
        }

        List<Long> foundJobIds = found.stream().map(MatchReport::getJobId).toList();
        List<Long> missing = ids.stream().filter(id -> !foundJobIds.contains(id)).toList();
        return new MatchReportDeleteVO(found.size(), missing);
    }

    /**
     * 按用户 + 岗位查已有报告，uk_user_job 保证最多一条
     */
    private MatchReport findByUserAndJob(Long userId, Long jobId) {
        return matchReportMapper.selectOne(Wrappers.<MatchReport>lambdaQuery()
                .eq(MatchReport::getUserId, userId)
                .eq(MatchReport::getJobId, jobId)
                .last("LIMIT 1"));
    }

    // 覆盖已有报告的正文与分数，updateTime 由 MybatisMetaObjectHandler 自动刷新
    private void updateContent(MatchReport report, int score, String content) {
        report.setScore(score);
        report.setContent(content);
        matchReportMapper.updateById(report);
    }

    /**
     * 将报告和岗位信息组合成VO
     */
    private MatchReportVO toVO(MatchReport report, JobPosition job) {
        MatchReportVO vo = new MatchReportVO();
        vo.setReportId(report.getId());
        vo.setJobId(report.getJobId());
        vo.setScore(report.getScore());
        vo.setSaveTime(report.getUpdateTime());
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

    /**
     * 从报告正文解析「## 综合匹配分」，解析不到按 0 处理
     */
    public static int parseMatchScore(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        Matcher matcher = MATCH_SCORE_PATTERN.matcher(content);
        if (!matcher.find()) {
            return 0;
        }
        int score = Integer.parseInt(matcher.group(1));
        return Math.min(100, Math.max(0, score));
    }

    /**
     * 从报告内容中提取技能覆盖率和薪资偏差
     */
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
