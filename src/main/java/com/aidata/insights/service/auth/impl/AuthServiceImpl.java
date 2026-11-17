package com.aidata.insights.service.auth.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.dto.auth.LoginRequest;
import com.aidata.insights.dto.auth.RegisterRequest;
import com.aidata.insights.entity.AppUser;
import com.aidata.insights.mapper.user.AppUserMapper;
import com.aidata.insights.security.JwtTokenProvider;
import com.aidata.insights.service.auth.IAuthService;
import com.aidata.insights.service.auth.IVerifyCodeService;
import com.aidata.insights.vo.auth.LoginVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * 认证服务：登录与注册
 *
 * <p>与既有 Node 服务保持一致的业务规则：
 * 注册需校验短信验证码（一次性，5 分钟内有效）；登录失败不区分「号码不存在」与「密码错误」；
 * 昵称未填时生成 job_ + 6 位随机字母数字。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String NICKNAME_PREFIX = "job_";

    private static final String NICKNAME_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserMapper appUserMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final IVerifyCodeService verifyCodeService;

    private final RedisService redisService;

    /**
     * 手机号 + 密码登录
     */
    @Override
    public LoginVO login(LoginRequest request) {
        AppUser user = appUserMapper.selectOne(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, request.getPhone())
                .last("LIMIT 1"));

        // 号码不存在与密码错误返回同一提示，避免暴露哪些号码已注册
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.LOGIN_FAILED);
        }

        log.info("用户登录成功: userId={}, phone={}", user.getId(), user.getPhone());
        return issueToken(user);
    }

    /**
     * 退出登录：把当前令牌的指纹写入黑名单，TTL 与令牌剩余有效期一致
     */
    @Override
    public void logout(String authorizationHeader) {
        jwtTokenProvider.resolveToken(authorizationHeader).ifPresent(token -> {
            try {
                // 解析令牌获取载荷
                JwtTokenProvider.JwtPayload payload = jwtTokenProvider.parseToken(token);
                long ttlSeconds = Duration.between(Instant.now(), payload.expiresAt()).getSeconds();
                if (ttlSeconds <= 0) {
                    // 已过期，本身就已失效，无需拉黑
                    return;
                }
                // 存指纹而不是明文令牌，避免 Redis 中直接拿到可用凭证
                redisService.set(RedisKeyConstant.tokenBlacklist(jwtTokenProvider.fingerprint(token)),
                        "1", Duration.ofSeconds(ttlSeconds));
                log.info("令牌已加入黑名单: userId={}, ttl={}s", payload.userId(), ttlSeconds);
            } catch (JwtException | IllegalArgumentException e) {
                // 签名错误或格式错误，无需拉黑
                log.debug("退出登录时令牌不可解析，忽略: {}", e.getMessage());
            }
        });
    }

    /**
     * 手机号 + 验证码 + 密码注册，成功后直接签发令牌
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        // 校验并消费验证码：验证通过立即失效，防止重复使用
        verifyCodeService.verifyAndConsume(request.getPhone(), request.getCode());

        boolean existed = appUserMapper.exists(Wrappers.<AppUser>lambdaQuery()
                .eq(AppUser::getPhone, request.getPhone()));
        if (existed) {
            throw new BizException(ResultCode.PHONE_EXISTS);
        }

        AppUser user = new AppUser();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname().trim()
                : createDefaultNickname());
        user.setAvatar(StringUtils.hasText(request.getAvatar()) ? request.getAvatar().trim() : null);

        try {
            appUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册时由唯一索引兜底
            throw new BizException(ResultCode.PHONE_EXISTS);
        }

        log.info("用户注册成功: userId={}, phone={}", user.getId(), user.getPhone());
        return issueToken(user);
    }

    private LoginVO issueToken(AppUser user) {
        JwtTokenProvider.TokenInfo tokenInfo = jwtTokenProvider.generateToken(user.getId(), user.getPhone());
        return LoginVO.builder()
                .token(tokenInfo.token())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpireSeconds())
                .build();
    }

    /**
     * 默认昵称：job_ + 6 位小写字母或数字
     */
    private String createDefaultNickname() {
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(NICKNAME_CHARS.charAt(RANDOM.nextInt(NICKNAME_CHARS.length())));
        }
        return NICKNAME_PREFIX + suffix;
    }
}
