package com.aidata.insights.service.auth;

/**
 * 短信发送通道
 *
 * <p>与验证码的生成/校验解耦，接入真实服务商时只需新增实现类，
 * 由 app.sms.provider 选择。</p>
 */
public interface ISmsSender {

    /**
     * 下发验证码短信
     *
     * @param phone 手机号
     * @param code  期望下发的验证码
     * @return 实际下发的验证码。部分通道（如阿里云短信认证服务）由服务端生成验证码，
     *         返回值可能与入参不同，调用方应以返回值为准写入缓存
     */
    String send(String phone, String code);
}
