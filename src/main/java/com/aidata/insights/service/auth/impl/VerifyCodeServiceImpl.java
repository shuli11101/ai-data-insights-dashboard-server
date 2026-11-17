package com.aidata.insights.service.auth.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.config.properties.SmsProperties;
import com.aidata.insights.service.auth.ISmsSender;
import com.aidata.insights.service.auth.IVerifyCodeService;
import com.aidata.insights.vo.auth.SmsCodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

/**
 * 短信验证码服务实现
 *
 * <p>验证码只存 Redis，不落库、不打业务日志正文；校验通过立即删除，保证一次性。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeServiceImpl implements IVerifyCodeService {

    private static final String CODE_CHARS = "0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedisService redisService;

    private final ISmsSender smsSender;

    private final SmsProperties smsProperties;

    @Override
    public SmsCodeVO send(String phone, String clientIp) {
        // 1. 同一手机号重发节流，setIfAbsent 失败说明还在冷却期
        String resendKey = RedisKeyConstant.verifyCodeResend(phone);
        if (!redisService.setIfAbsent(resendKey, "1", smsProperties.getResendInterval())) {
            long remaining = Math.max(redisService.getExpire(resendKey), 1);
            throw new BizException(ResultCode.SMS_SEND_TOO_FREQUENT,
                    "发送太频繁，请 " + remaining + " 秒后重试");
        }

        // 2. IP 维度限流，防止换号刷短信
        limitByIp(clientIp);

        // 3. 手机号每日上限
        limitDaily(phone);

        // 4. 生成并下发；部分通道（阿里云短信认证服务）由服务端生成验证码，以返回值为准
        String codeKey = RedisKeyConstant.verifyCode(phone);
        String actualCode;
        try {
            actualCode = smsSender.send(phone, generateCode());
        } catch (RuntimeException e) {
            // 发送失败时不占用冷却时间，允许用户立即重试
            redisService.delete(resendKey);
            log.error("验证码发送失败: phone={}", phone, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }

        // 5. 写入实际下发的验证码，注册时校验
        redisService.set(codeKey, actualCode, smsProperties.getCodeTtl());

        SmsCodeVO vo = new SmsCodeVO();
        vo.setExpiresIn(smsProperties.getCodeTtl().toSeconds());
        vo.setResendInterval(smsProperties.getResendInterval().toSeconds());
        return vo;
    }

    @Override
    public void verifyAndConsume(String phone, String code) {
        String codeKey = RedisKeyConstant.verifyCode(phone);
        Optional<String> saved = redisService.get(codeKey);
        if (saved.isEmpty()) {
            throw new BizException(ResultCode.VERIFY_CODE_EXPIRED);
        }
        if (!saved.get().equals(code)) {
            recordFailure(phone);
            throw new BizException(ResultCode.VERIFY_CODE_ERROR);
        }

        // 一次性消费，防止同一验证码重复使用
        redisService.delete(codeKey);
        redisService.delete(RedisKeyConstant.verifyCodeFail(phone));
    }

    /**
     * 失败次数超限后作废验证码，避免 6 位码被枚举
     */
    private void recordFailure(String phone) {
        String failKey = RedisKeyConstant.verifyCodeFail(phone);
        long failures = redisService.increment(failKey, 1);
        if (failures == 1) {
            redisService.expire(failKey, smsProperties.getFailWindow());
        }
        if (failures >= smsProperties.getMaxVerifyAttempts()) {
            redisService.delete(RedisKeyConstant.verifyCode(phone));
            redisService.delete(failKey);
            log.warn("验证码失败次数过多已作废: phone={}", phone);
            throw new BizException(ResultCode.VERIFY_CODE_EXPIRED, "验证码错误次数过多，请重新获取");
        }
    }

    private void limitByIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return;
        }
        String ipKey = RedisKeyConstant.verifyCodeIp(clientIp);
        long count = redisService.increment(ipKey, 1);
        if (count == 1) {
            redisService.expire(ipKey, Duration.ofHours(1));
        }
        if (count > smsProperties.getIpHourlyLimit()) {
            log.warn("验证码请求过于频繁: ip={}, count={}", clientIp, count);
            throw new BizException(ResultCode.SMS_SEND_TOO_FREQUENT, "请求过于频繁，请稍后再试");
        }
    }

    private void limitDaily(String phone) {
        String dailyKey = RedisKeyConstant.verifyCodeDaily(phone);
        long count = redisService.increment(dailyKey, 1);
        if (count == 1) {
            redisService.expire(dailyKey, Duration.ofDays(1));
        }
        if (count > smsProperties.getDailyLimit()) {
            throw new BizException(ResultCode.SMS_DAILY_LIMIT);
        }
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(smsProperties.getCodeLength());
        for (int i = 0; i < smsProperties.getCodeLength(); i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }
}
