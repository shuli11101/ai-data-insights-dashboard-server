package com.aidata.insights.service.auth.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.config.properties.SmsProperties;
import com.aidata.insights.service.auth.ISmsSender;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信认证服务通道（号码认证 dypnsapi）
 *
 * <p>启用方式：app.sms.provider=aliyun-auth。个人实名认证账号可用，
 * 使用控制台赠送签名与赠送模板（100001~100005），无需申请资质。</p>
 *
 * <p>该接口支持服务端生成验证码：此处传入自定义验证码，并要求响应返回实际下发的验证码，
 * 若服务端改写则以返回值为准，保证与缓存中的验证码一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun-auth")
public class AliyunSmsAuthSender implements ISmsSender {

    private final Client aliyunSmsAuthClient;

    private final SmsProperties smsProperties;

    @Override
    public String send(String phone, String code) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();

        // 赠送模板包含两个变量：验证码 code + 有效期 min（分钟），缺任一都会报 isv.INVALID_PARAMETERS
        String templateParam = "{\"" + aliyun.getTemplateParamName() + "\":\"" + code + "\","
                + "\"" + aliyun.getValidityParamName() + "\":\""
                + smsProperties.getCodeTtl().toMinutes() + "\"}";

        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName(aliyun.getSignName())
                // 赠送模板编号，如 100001（登录/注册场景）
                .setTemplateCode(aliyun.getTemplateCode())
                // 自定义验证码 + 有效期，与本地校验保持一致
                .setTemplateParam(templateParam)
                .setValidTime(smsProperties.getCodeTtl().toSeconds())
                .setInterval(smsProperties.getResendInterval().toSeconds())
                // 返回实际下发的验证码，服务端自行生成时也能保持一致
                .setReturnVerifyCode(true);

        // scheme-name 非必填，不填时使用阿里云默认方案
        if (StringUtils.hasText(aliyun.getSchemeName())) {
            request.setSchemeName(aliyun.getSchemeName());
        }

        try {
            SendSmsVerifyCodeResponse response = aliyunSmsAuthClient.sendSmsVerifyCode(request);
            String bizCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(bizCode)) {
                // 常见失败码：isv.BUSINESS_LIMIT_CONTROL、biz.FREQUENCY、isv.SMS_SIGNATURE_ILLEGAL
                log.error("阿里云短信认证下发失败: phone={}, code={}, message={}, requestId={}",
                        phone, bizCode,
                        response.getBody() == null ? null : response.getBody().getMessage(),
                        response.getBody() == null ? null : response.getBody().getRequestId());
                throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
            }

            String issuedCode = response.getBody().getModel() == null
                    ? null
                    : response.getBody().getModel().getVerifyCode();
            log.info("阿里云短信认证下发成功: phone={}, requestId={}", phone, response.getBody().getRequestId());
            return StringUtils.hasText(issuedCode) ? issuedCode : code;
        } catch (TeaException e) {
            log.error("阿里云短信认证调用异常: phone={}, errorCode={}, message={}", phone, e.getCode(), e.getMessage());
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        } catch (Exception e) {
            log.error("阿里云短信认证调用异常: phone={}", phone, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }
    }
}
