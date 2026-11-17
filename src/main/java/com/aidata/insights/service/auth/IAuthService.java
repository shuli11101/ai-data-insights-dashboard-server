package com.aidata.insights.service.auth;

import com.aidata.insights.dto.auth.LoginRequest;
import com.aidata.insights.dto.auth.RegisterRequest;
import com.aidata.insights.vo.auth.LoginVO;

public interface IAuthService {

    /**
     * 手机号 + 密码登录
     */
    LoginVO login(LoginRequest request);

    /**
     * 退出登录：把当前令牌加入黑名单，直到令牌自然过期
     *
     * @param authorizationHeader 原始 Authorization 头（含 Bearer 前缀）
     */
    void logout(String authorizationHeader);

    /**
     * 手机号 + 验证码 + 密码注册，成功后直接签发令牌
     */
    LoginVO register(RegisterRequest request);
}
