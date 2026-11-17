package com.aidata.insights.service.auth;

import com.aidata.insights.vo.auth.SmsCodeVO;

/**
 * 短信验证码服务：生成、下发、校验
 */
public interface IVerifyCodeService {

    /**
     * 生成并发送验证码
     *
     * @param phone    手机号
     * @param clientIp 请求来源 IP，用于限流
     * @return 有效期与重发间隔
     */
    SmsCodeVO send(String phone, String clientIp);

    /**
     * 校验并消费验证码，通过后立即失效（一次性）
     *
     * @param phone 手机号
     * @param code  用户提交的验证码
     */
    void verifyAndConsume(String phone, String code);
}
