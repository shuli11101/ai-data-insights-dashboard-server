package com.aidata.insights.service.auth.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.config.properties.SmsProperties;
import com.aidata.insights.service.auth.ISmsSender;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信通道
 *
 * <p>启用方式：app.sms.provider=aliyun，并配置 AccessKey、签名与验证码模板。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsSender implements ISmsSender {

    private final Client aliyunSmsClient;

    private final SmsProperties smsProperties;

    @Override
    public String send(String phone, String code) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();

        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(aliyun.getSignName())
                .setTemplateCode(aliyun.getTemplateCode())
                // 模板变量，模板内容形如：您的验证码为 ${code}，5 分钟内有效
                .setTemplateParam("{\"" + aliyun.getTemplateParamName() + "\":\"" + code + "\"}");

        try {
            SendSmsResponse response = aliyunSmsClient.sendSms(request);
            String bizCode = response.getBody() == null ? null : response.getBody().getCode();
            if (!"OK".equals(bizCode)) {
                // 常见失败码：isv.BUSINESS_LIMIT_CONTROL（触发流控）、isv.MOBILE_NUMBER_ILLEGAL、isv.SMS_SIGNATURE_ILLEGAL
                log.error("阿里云短信下发失败: phone={}, code={}, message={}, requestId={}",
                        phone, bizCode,
                        response.getBody() == null ? null : response.getBody().getMessage(),
                        response.getBody() == null ? null : response.getBody().getRequestId());
                throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
            }
            log.info("阿里云短信下发成功: phone={}, requestId={}", phone, response.getBody().getRequestId());
            return code;
        } catch (TeaException e) {
            // 网络异常、AK 无权限等，错误码在 e.getCode() 中
            log.error("阿里云短信调用异常: phone={}, errorCode={}, message={}", phone, e.getCode(), e.getMessage());
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        } catch (Exception e) {
            log.error("阿里云短信调用异常: phone={}", phone, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }
    }
}
