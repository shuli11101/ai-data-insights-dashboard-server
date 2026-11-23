package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.trace.TraceContext;
import com.aidata.insights.config.properties.AiProperties;
import com.aidata.insights.dto.ai.AiMatchRequest;
import com.aidata.insights.entity.JobIntention;
import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.entity.MatchReport;
import com.aidata.insights.mapper.home.JobPositionMapper;
import com.aidata.insights.mapper.personalInfo.IJobIntentionMapper;
import com.aidata.insights.mapper.user.MatchReportMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.ai.AiStreamClient;
import com.aidata.insights.service.ai.IAiMatchService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位匹配度分析服务
 *
 * <p>服务端自动取岗位信息 + 当前用户的求职意向交给 AI 打分。
 * 只做分析不落库，用户点「保存报告」后由 POST /user/match-report 写入。</p>
 *
 * <p>SSE 帧与前端契约一致：首帧 {@code {"cached":true,"score":x}} 或 {@code {"cached":false}}，
 * 随后 {@code {"content":"..."}}，结束 {@code [DONE]}，异常 {@code {"error":"..."}}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMatchServiceImpl implements IAiMatchService {

    private static final String ROLE_SYSTEM = "system";

    private static final String ROLE_USER = "user";

    private static final String SCENE_MATCH = "match";

    /** 线程池饱和时的提示文案：流已开始，只能以 error 帧下发 */
    private static final String BUSY_MESSAGE = "当前使用人数较多，请稍后再试";

    private final AiProperties aiProperties;

    private final AiStreamClient aiStreamClient;

    private final JobPositionMapper jobPositionMapper;

    private final IJobIntentionMapper jobIntentionMapper;

    private final MatchReportMapper matchReportMapper;

    /** AI 流式任务共享线程池：有界队列，饱和时快速失败，由 Spring 统一管理生命周期 */
    private final ThreadPoolTaskExecutor aiStreamExecutor;

    @Override
    public SseEmitter streamMatch(AiMatchRequest request) {
        Long userId = SecurityUtils.requireUserId();

        JobPosition job = jobPositionMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BizException(ResultCode.NOT_FOUND, "岗位不存在");
        }

        JobIntention preference = jobIntentionMapper.selectById(userId);
        if (preference == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先填写求职意向");
        }

        // force=true 时跳过缓存重新生成；命中缓存时把上次正文一次性下发，前端不用分两种情况
        MatchReport cached = Boolean.TRUE.equals(request.getForce()) ? null : findReport(userId, request.getJobId());

        SseEmitter emitter = aiStreamClient.newEmitter();
        if (cached != null) {
            aiStreamClient.sendData(emitter, Map.of(
                    "cached", true,
                    "score", cached.getScore() == null ? 0 : cached.getScore()));
            aiStreamClient.sendData(emitter, Map.of("content", cached.getContent() == null ? "" : cached.getContent()));
            aiStreamClient.sendDone(emitter);
            aiStreamClient.complete(emitter);
            return emitter;
        }

        List<Map<String, String>> payload = buildPayload(job, preference);
        // 匹配不落会话，合成一个业务标识仍随请求体下发，避免改动与 FastAPI 的请求契约
        String conversationId = "match-" + userId + "-" + request.getJobId();
        // 本次调用的追踪标识，每次请求都不同，可与 Java/FastAPI 日志一一对应
        String traceId = TraceContext.currentOrNew();

        aiStreamClient.sendData(emitter, Map.of("cached", false));
        // 提交异步任务，traceId 由线程池的 TaskDecorator 透传到子线程
        try {
            aiStreamExecutor.execute(() -> {
                try {
                    aiStreamClient.stream(userId, conversationId, traceId, SCENE_MATCH, payload,
                            delta -> aiStreamClient.sendData(emitter, Map.of("content", delta)));
                    aiStreamClient.sendDone(emitter);
                } catch (Exception e) {
                    log.error("岗位匹配分析失败: trace={}, jobId={}", traceId, request.getJobId(), e);
                    aiStreamClient.sendError(emitter, "AI 分析失败，请稍后重试");
                } finally {
                    aiStreamClient.complete(emitter);
                }
            });
        } catch (TaskRejectedException rejected) {
            // 线程池饱和：响应头已发出，只能以 error 帧告知并关闭连接，否则前端会一直等
            log.error("岗位匹配任务被拒绝（线程池饱和）: trace={}, jobId={}", traceId, request.getJobId());
            aiStreamClient.sendError(emitter, BUSY_MESSAGE);
            aiStreamClient.complete(emitter);
        }
        return emitter;
    }

    private MatchReport findReport(Long userId, Long jobId) {
        return matchReportMapper.selectOne(Wrappers.<MatchReport>lambdaQuery()
                .eq(MatchReport::getUserId, userId)
                .eq(MatchReport::getJobId, jobId)
                .last("LIMIT 1"));
    }

    /**
     * 组装匹配用的消息：基础 system 提示 + 岗位信息，求职者信息作为补充信息拼在末尾。
     * 场景提示词后续在 FastAPI 侧统一编排。
     */
    private List<Map<String, String>> buildPayload(JobPosition job, JobIntention preference) {
        List<Map<String, String>> payload = new ArrayList<>(2);
        if (StringUtils.hasText(aiProperties.getSystemPrompt())) {
            payload.add(Map.of("role", ROLE_SYSTEM, "content", aiProperties.getSystemPrompt()));
        }
        payload.add(Map.of("role", ROLE_USER,
                "content", formatJobPrompt(job) + "\n\n补充信息：\n" + formatPreferencePrompt(preference)));
        return payload;
    }

    /**
     * 岗位信息转成提示词正文
     */
    private String formatJobPrompt(JobPosition job) {
        List<String> skills = job.getSkillTags() == null ? List.of() : job.getSkillTags().stream()
                .filter(StringUtils::hasText)
                .toList();
        String district = StringUtils.hasText(job.getDistrict()) ? "（" + job.getDistrict() + "）" : "";
        int salaryMin = job.getSalaryMin() == null ? 0 : job.getSalaryMin();
        int salaryMax = job.getSalaryMax() == null ? 0 : job.getSalaryMax();

        return String.join("\n",
                "岗位信息：",
                "- 职位名称：" + text(job.getJobName()),
                "- 公司：" + text(job.getCompany()),
                "- 城市：" + text(job.getCity()) + district,
                "- 薪资：" + salaryMin + "k-" + salaryMax + "k",
                "- 经验要求：" + (StringUtils.hasText(job.getExperience()) ? job.getExperience() : "未提及"),
                "- 技能标签：" + (skills.isEmpty() ? "未提及" : String.join("、", skills)));
    }

    /**
     * 求职者信息转成提示词里的补充信息
     */
    private String formatPreferencePrompt(JobIntention preference) {
        List<String> skills = preference.getSkillTags() == null ? List.of() : preference.getSkillTags().stream()
                .filter(StringUtils::hasText)
                .toList();

        return String.join("\n",
                "求职者信息：",
                "- 意向城市：" + orUnfilled(preference.getCity()),
                "- 意向岗位：" + orUnfilled(preference.getJobTitle()),
                "- 工作经验：" + orUnfilled(preference.getExperience()),
                "- 学历：" + orUnfilled(preference.getEducation()),
                "- 掌握技能：" + (skills.isEmpty() ? "未填写" : String.join("、", skills)),
                "- 当前月薪：" + salaryText(preference.getCurrentSalary()),
                "- 期望月薪：" + salaryText(preference.getExpectSalary()),
                "- 求职状态：" + orUnfilled(preference.getJobStatus()));
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value : "未提及";
    }

    private String orUnfilled(String value) {
        return StringUtils.hasText(value) ? value : "未填写";
    }

    private String salaryText(Integer salary) {
        return salary == null ? "未填写" : salary + " 元";
    }
}