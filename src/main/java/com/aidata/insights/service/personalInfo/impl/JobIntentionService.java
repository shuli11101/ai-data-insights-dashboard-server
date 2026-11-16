package com.aidata.insights.service.personalInfo.impl;

import com.aidata.insights.dto.personalInfo.SaveJobIntentionRequest;
import com.aidata.insights.entity.JobIntention;
import com.aidata.insights.mapper.personalInfo.IJobIntentionMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.personalInfo.IJobIntentionService;
import com.aidata.insights.vo.personalInfo.JobIntentionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/** 求职意向服务实现 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobIntentionService implements IJobIntentionService {

    private final IJobIntentionMapper jobIntentionMapper;

    /**
     * 获取当前用户的求职意向，未填写时返回 null
     */
    @Override
    public JobIntentionVO getPersonal() {
        // 获取用户id
        Long userId = SecurityUtils.requireUserId();

        // 表主键是 user_id，直接按主键查，一个用户最多一条
        JobIntention intention = jobIntentionMapper.selectById(userId);
        return intention == null ? null : toVO(intention);
    }

    /**
     * 获取当前用户的求职意向状态，未填写时返回 false
     */
    @Override
    public boolean getSurveyStatus() {
        // 获取用户id
        Long userId = SecurityUtils.requireUserId();
        // 表主键是 user_id，直接按主键查，一个用户最多一条
        JobIntention intention = jobIntentionMapper.selectById(userId);
        return intention != null;
    }

    /**
     * 保存 / 更新当前用户的求职意向，返回保存后的最新数据
     */
    @Override
    public JobIntentionVO setPersonalInfo(SaveJobIntentionRequest request) {
        // 获取用户id
        Long userId = SecurityUtils.requireUserId();

        // 主键就是 user_id：有记录则更新，没有则新增
        JobIntention intention = jobIntentionMapper.selectById(userId);
        if (intention == null) {
            intention = new JobIntention();
            intention.setUserId(userId);
            copy(request, intention);
            try {
                jobIntentionMapper.insert(intention);
            } catch (DuplicateKeyException e) {
                // 并发提交时另一个请求已插入，转为覆盖更新
                jobIntentionMapper.updateById(intention);
            }
        } else {
            copy(request, intention);
            jobIntentionMapper.updateById(intention);
        }
        return toVO(intention);
    }

    /**
     * 请求字段 -> 实体字段（skills 落 skill_tags，expectedSalary 落 expect_salary）
     */
    private void copy(SaveJobIntentionRequest request, JobIntention intention) {
        intention.setCity(request.getCity());
        intention.setJobTitle(request.getJobTitle());
        intention.setExperience(request.getExperience());
        intention.setEducation(request.getEducation());
        intention.setSkillTags(request.getSkillTags());
        intention.setCurrentSalary(request.getCurrentSalary());
        intention.setExpectSalary(request.getExpectedSalary());
        intention.setJobStatus(request.getJobStatus());
    }

    /**
     * 实体转 VO，字段一一对应
     */
    private JobIntentionVO toVO(JobIntention intention) {
        JobIntentionVO vo = new JobIntentionVO();
        vo.setCity(intention.getCity());
        vo.setJobTitle(intention.getJobTitle());
        vo.setExperience(intention.getExperience());
        vo.setEducation(intention.getEducation());
        vo.setSkills(intention.getSkillTags());
        vo.setCurrentSalary(intention.getCurrentSalary());
        vo.setExpectedSalary(intention.getExpectSalary());
        vo.setJobStatus(intention.getJobStatus());
        vo.setUpdateTime(intention.getUpdateTime());
        return vo;
    }
}
