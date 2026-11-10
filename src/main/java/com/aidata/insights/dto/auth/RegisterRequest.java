package com.aidata.insights.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求（手机号 + 验证码 + 密码）
 *
 * <p>短信服务接入前，验证码固定为 123456，与既有服务保持一致。</p>
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    @Schema(description = "短信验证码，当前固定 123456", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入验证码")
    private String code;

    @Schema(description = "密码：8-32 位，不能有空格，且同时包含字母和数字", example = "abc12345",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,32}$", message = "密码需为 8-32 位，且同时包含字母和数字")
    private String password;

    @Schema(description = "昵称，可选，最多 20 个字")
    @Size(max = 20, message = "昵称最多 20 个字")
    private String nickname;

    @Schema(description = "头像地址，可选")
    @Size(max = 500, message = "头像地址过长")
    private String avatar;
}
