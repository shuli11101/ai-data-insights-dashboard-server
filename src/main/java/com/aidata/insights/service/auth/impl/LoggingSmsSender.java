package com.aidata.insights.service.auth.impl;

import com.aidata.insights.service.auth.ISmsSender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 日志通道：只把验证码打到日志，不真正下发短信
 *
 * <p>用于本地开发与联调；生产环境必须切换为真实短信服务商。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements ISmsSender {

    private final Environment environment;

    @PostConstruct
    void warnIfProduction() {
        if (List.of(environment.getActiveProfiles()).contains("prod")) {
            log.warn("app.sms.provider=log：验证码只会打印到日志，不会真实下发，生产环境请切换为真实短信服务商");
        }
    }

    @Override
    public String send(String phone, String code) {
        log.info("[短信-日志通道] phone={}, code={}", phone, code);
        return code;
    }
}
