package com.aidata.insights.controller.auth;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.dto.auth.LoginRequest;
import com.aidata.insights.dto.auth.RegisterRequest;
import com.aidata.insights.service.auth.AuthService;
import com.aidata.insights.vo.auth.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final AuthService authService;

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
}
