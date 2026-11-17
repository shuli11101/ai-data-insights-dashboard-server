package com.aidata.insights.config;

import com.aidata.insights.config.properties.SmsProperties;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信认证服务客户端配置，仅在 app.sms.provider=aliyun-auth 时装配
 *
 * <p>与常规短信服务（dysmsapi）不同：短信认证服务走号码认证产品线，
 * 支持个人实名认证、免申请资质，使用系统赠送签名与 100001~100005 赠送模板。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun-auth")
public class AliyunSmsAuthConfig {

    @Bean
    public Client aliyunSmsAuthClient(SmsProperties smsProperties) throws Exception {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        require(aliyun.getAccessKeyId(), "app.sms.aliyun.access-key-id");
        require(aliyun.getAccessKeySecret(), "app.sms.aliyun.access-key-secret");
        require(aliyun.getSignName(), "app.sms.aliyun.sign-name（填控制台赠送的签名名称）");
        require(aliyun.getTemplateCode(), "app.sms.aliyun.template-code（赠送模板编号 100001~100005）");
        // scheme-name 非必填，不填时阿里云使用“默认方案”
        if (!StringUtils.hasText(aliyun.getSchemeName())) {
            log.info("未配置 app.sms.aliyun.scheme-name，将使用阿里云默认方案");
        }

        Config config = new Config()
                .setAccessKeyId(aliyun.getAccessKeyId())
                .setAccessKeySecret(aliyun.getAccessKeySecret())
                .setEndpoint(aliyun.getAuthEndpoint());

        log.info("阿里云短信认证服务已启用: signName={}, templateCode={}, schemeName={}, endpoint={}",
                aliyun.getSignName(), aliyun.getTemplateCode(), aliyun.getSchemeName(), aliyun.getAuthEndpoint());
        return new Client(config);
    }

    private void require(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("app.sms.provider=aliyun-auth 时必须配置 " + key);
        }
    }
}
