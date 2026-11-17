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
import com.aidata.insights.service.personalInfo.IAvatarStorageService;
import com.aidata.insights.service.personalInfo.IPersonalInfoService;
import com.aidata.insights.vo.personalInfo.AvatarVO;
import com.aidata.insights.vo.personalInfo.PersonalInfoVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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

    private final IAvatarStorageService avatarStorageService;

    /**
     * context-path，用于把库里的相对地址拼成可直接访问的地址
     */
    @Value("${server.servlet.context-path:}")
    private String contextPath;

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
        vo.setAvatar(toPublicUrl(user.getAvatar()));
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

    @Override
    public PersonalInfoVO updatePersonalInfo(PersonalInfoVO personalInfoVO) {
        // 获取当前登录用户 ID
        Long userId = SecurityUtils.requireUserId();

        // 只更新有传值的字段（null 表示不修改）；手机号是登录账号，不允许在本接口变更
        LambdaUpdateWrapper<AppUser> update = Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId)
                // update(null, wrapper) 不会触发 strictUpdateFill，更新时间需显式赋值
                .set(AppUser::getUpdateTime, LocalDateTime.now());
        if (personalInfoVO.getNickname() != null) {
            update.set(AppUser::getNickname, personalInfoVO.getNickname());
        }
        if (personalInfoVO.getAvatar() != null) {
            update.set(AppUser::getAvatar, normalizeAvatar(personalInfoVO.getAvatar()));
        }
        if (personalInfoVO.getCurrentJob() != null) {
            update.set(AppUser::getCurrentJob, personalInfoVO.getCurrentJob());
        }
        if (personalInfoVO.getLocation() != null) {
            update.set(AppUser::getLocation, personalInfoVO.getLocation());
        }

        // 受影响行数为 0 说明当前用户已不存在
        if (appUserMapper.update(null, update) == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 工作经验只在求职意向接口维护，资料更新不处理
        return getPersonalInfo();
    }

    @Override
    public AvatarVO uploadAvatar(MultipartFile file) {
        Long userId = SecurityUtils.requireUserId();

        // 存盘后直接落库，前端不需要再调一次资料更新接口
        String avatar = avatarStorageService.store(userId, file);
        if (appUserMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                .eq(AppUser::getId, userId)
                .set(AppUser::getAvatar, avatar)
                .set(AppUser::getUpdateTime, LocalDateTime.now())) == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        AvatarVO vo = new AvatarVO();
        vo.setAvatar(avatar);
        vo.setUrl(toPublicUrl(avatar));
        return vo;
    }

    /**
     * 库内相对地址 -> 可直接访问的地址；已是 http(s) 或已带 context-path 时保持原样
     */
    private String toPublicUrl(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return null;
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        String prefix = contextPathPrefix();
        if (!prefix.isEmpty() && avatar.startsWith(prefix + "/")) {
            return avatar;
        }
        return prefix + avatar;
    }

    /**
     * 入库前去掉 context-path，保证库内统一存相对地址
     */
    private String normalizeAvatar(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return null;
        }
        String prefix = contextPathPrefix();
        if (!prefix.isEmpty() && avatar.startsWith(prefix + "/")) {
            return avatar.substring(prefix.length());
        }
        return avatar;
    }

    private String contextPathPrefix() {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.endsWith("/")
                ? contextPath.substring(0, contextPath.length() - 1)
                : contextPath;
    }
}
