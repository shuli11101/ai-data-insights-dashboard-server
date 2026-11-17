package com.aidata.insights.config;

import com.aidata.insights.config.properties.SmsProperties;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信客户端配置，仅在 app.sms.provider=aliyun 时装配
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.sms", name = "provider", havingValue = "aliyun")
public class AliyunSmsConfig {

    @Bean
    public Client aliyunSmsClient(SmsProperties smsProperties) throws Exception {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        // 缺配置时启动直接失败，避免线上静默退化成"发不出去也看不出来"
        require(aliyun.getAccessKeyId(), "app.sms.aliyun.access-key-id");
        require(aliyun.getAccessKeySecret(), "app.sms.aliyun.access-key-secret");
        require(aliyun.getSignName(), "app.sms.aliyun.sign-name");
        require(aliyun.getTemplateCode(), "app.sms.aliyun.template-code");

        Config config = new Config()
                .setAccessKeyId(aliyun.getAccessKeyId())
                .setAccessKeySecret(aliyun.getAccessKeySecret())
                .setEndpoint(aliyun.getEndpoint());

        log.info("阿里云短信通道已启用: signName={}, templateCode={}, endpoint={}",
                aliyun.getSignName(), aliyun.getTemplateCode(), aliyun.getEndpoint());
        return new Client(config);
    }

    private void require(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("app.sms.provider=aliyun 时必须配置 " + key);
        }
    }
}
