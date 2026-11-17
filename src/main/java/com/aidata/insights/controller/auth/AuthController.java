package com.aidata.insights.controller.auth;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.auth.LoginRequest;
import com.aidata.insights.dto.auth.RegisterRequest;
import com.aidata.insights.dto.auth.SendSmsCodeRequest;
import com.aidata.insights.service.auth.IAuthService;
import com.aidata.insights.service.auth.IVerifyCodeService;
import com.aidata.insights.vo.auth.LoginVO;
import com.aidata.insights.vo.auth.SmsCodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * <p>路径与既有服务一致：POST /api/auth/register、POST /api/auth/login</p>
 */
@Tag(name = "01. 认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    private final IVerifyCodeService verifyCodeService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public Result<SmsCodeVO> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request,
                                         HttpServletRequest servletRequest) {
        // 经 Spring 的转发头处理后，getRemoteAddr 取到的是真实客户端 IP
        String clientIp = servletRequest.getRemoteAddr();
        return Result.ok(verifyCodeService.send(request.getPhone(), clientIp), "验证码已发送");
    }

    @Operation(summary = "注册：手机号 + 验证码 + 密码")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录：手机号 + 密码")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 退出登录：当前令牌立即失效
     */
    @Operation(summary = "退出登录：当前令牌加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.<Void>ok(null, "已退出登录");
    }
}
