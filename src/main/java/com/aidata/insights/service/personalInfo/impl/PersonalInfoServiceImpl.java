package com.aidata.insights.service.personalInfo.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.entity.AppUser;
import com.aidata.insights.entity.JobIntention;
import com.aidata.insights.entity.MatchReport;
import com.aidata.insights.entity.UserFavorite;
import com.aidata.insights.mapper.personalInfo.IJobIntentionMapper;
import com.aidata.insights.mapper.user.AppUserMapper;
import com.aidata.insights.mapper.user.MatchReportMapper;
import com.aidata.insights.mapper.user.UserFavoriteMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.service.personalInfo.IPersonalInfoService;
import com.aidata.insights.vo.personalInfo.PersonalInfoVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 个人信息服务
 *
 * <p>聚合 app_user 的资料、user_preference 的经验，以及匹配报告、收藏的统计数。</p>
 */
@Service
@RequiredArgsConstructor
public class PersonalInfoServiceImpl implements IPersonalInfoService {

    private final AppUserMapper appUserMapper;

    private final IJobIntentionMapper jobIntentionMapper;

    private final MatchReportMapper matchReportMapper;

    private final UserFavoriteMapper userFavoriteMapper;

    @Override
    public PersonalInfoVO getPersonalInfo() {
        Long userId = SecurityUtils.requireUserId();

        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        PersonalInfoVO vo = new PersonalInfoVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCurrentJob(user.getCurrentJob());
        vo.setLocation(user.getLocation());

        // 工作经验存在求职意向表，未填写时为 null
        JobIntention intention = jobIntentionMapper.selectById(userId);
        vo.setExperience(intention == null ? null : intention.getExperience());

        // 报告数量与平均分：只取 score 列，避免把 Markdown 正文全部查出来
        List<MatchReport> reports = matchReportMapper.selectList(
                Wrappers.<MatchReport>lambdaQuery()
                        .select(MatchReport::getScore)
                        .eq(MatchReport::getUserId, userId));
        vo.setReportCount(reports.size());
        vo.setAverageScore((int) Math.round(reports.stream()
                .filter(report -> report.getScore() != null)
                .mapToInt(MatchReport::getScore)
                .average()
                .orElse(0)));

        Long favoriteCount = userFavoriteMapper.selectCount(
                Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId));
        vo.setFavoriteCount(favoriteCount == null ? 0 : favoriteCount.intValue());

        return vo;
    }
}
