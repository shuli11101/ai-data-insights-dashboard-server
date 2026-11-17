package com.aidata.insights.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 短信验证码配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    /**
     * 发送通道：log 只打印日志（开发用），接入真实短信服务商后新增对应实现
     */
    @NotBlank
    private String provider = "log";

    /**
     * 验证码位数
     */
    @Min(4)
    private int codeLength = 6;

    /**
     * 验证码有效期
     */
    private Duration codeTtl = Duration.ofMinutes(5);

    /**
     * 同一手机号的重发间隔
     */
    private Duration resendInterval = Duration.ofSeconds(60);

    /**
     * 同一手机号每日发送上限，防止被刷费用
     */
    @Min(1)
    private int dailyLimit = 10;

    /**
     * 同一验证码允许的最大校验失败次数，超过后验证码作废
     */
    @Min(1)
    private int maxVerifyAttempts = 5;

    /**
     * 失败计数窗口，窗口内累计达到上限即作废
     */
    private Duration failWindow = Duration.ofMinutes(10);

    /**
     * 同一 IP 每小时发送上限
     */
    @Min(1)
    private int ipHourlyLimit = 20;

    /**
     * 阿里云短信通道配置，provider=aliyun 时生效
     */
    private Aliyun aliyun = new Aliyun();

    /**
     * 阿里云短信服务参数
     */
    @Data
    public static class Aliyun {

        /**
         * 建议使用只授予短信权限的 RAM 子账号
         */
        private String accessKeyId;

        private String accessKeySecret;

        /**
         * 短信签名，需在控制台审核通过
         */
        private String signName;

        /**
         * 验证码模板 CODE
         *
         * <p>常规短信服务为 SMS_ 开头；短信认证服务（provider=aliyun-auth）为赠送模板编号 100001~100005。</p>
         */
        private String templateCode;

        /**
         * 短信认证服务的方案名称，在号码认证控制台创建方案后获得（provider=aliyun-auth 时必填）
         */
        private String schemeName;

        /**
         * 模板中验证码变量名，模板内容为 ${code} 时保持默认值
         */
        private String templateParamName = "code";

        /**
         * 赠送模板中有效期变量名（分钟），赠送模板同时包含验证码与有效期两个变量
         */
        private String validityParamName = "min";

        /**
         * 常规短信服务接入地址（provider=aliyun）
         */
        private String endpoint = "dysmsapi.aliyuncs.com";

        /**
         * 短信认证服务接入地址（provider=aliyun-auth），与常规短信服务不是同一个网关
         */
        private String authEndpoint = "dypnsapi.aliyuncs.com";
    }
}
