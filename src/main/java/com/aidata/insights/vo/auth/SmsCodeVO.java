package com.aidata.insights.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 验证码发送结果
 */
@Data
@Schema(description = "验证码发送结果")
public class SmsCodeVO {

    @Schema(description = "验证码有效期（秒）", example = "300")
    private long expiresIn;

    @Schema(description = "允许重发的间隔（秒）", example = "60")
    private long resendInterval;
}
