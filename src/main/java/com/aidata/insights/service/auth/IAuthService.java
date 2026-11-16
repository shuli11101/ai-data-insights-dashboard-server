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
     * 手机号 + 验证码 + 密码注册，成功后直接签发令牌
     */
    LoginVO register(RegisterRequest request);
}
