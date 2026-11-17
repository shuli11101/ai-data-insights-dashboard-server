package com.aidata.insights.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 文件上传配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /**
     * 头像存储方式：local 本地磁盘（开发默认），s3 对象存储（线上）
     */
    @NotBlank
    private String storageType = "local";

    /**
     * 对象存储配置，storageType=s3 时生效
     */
    private S3 s3 = new S3();

    /**
     * 上传文件根目录，静态资源映射 /uploads/** 指向它；avatarDir 应位于其下
     */
    @NotBlank
    private String rootDir = "./uploads";

    /**
     * 头像存储根目录，生产环境建议指向挂载的持久化卷，如 /data/uploads/avatar
     */
    @NotBlank
    private String avatarDir = "./uploads/avatar";

    /**
     * 头像访问前缀，相对 context-path，需与静态资源映射保持一致
     */
    @NotBlank
    private String avatarUrlPrefix = "/uploads/avatar";

    /**
     * 单个头像大小上限（字节），默认 2MB
     */
    @Min(1)
    private long avatarMaxSize = 2 * 1024 * 1024;

    /**
     * 允许的图片扩展名，小写，不含点
     */
    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp");

    /**
     * S3 协议对象存储（MinIO / 阿里云 OSS / 腾讯云 COS）
     */
    @Data
    public static class S3 {

        /**
         * 服务地址，MinIO 形如 http://127.0.0.1:9000
         */
        private String endpoint;

        private String accessKey;

        private String secretKey;

        /**
         * 桶名，不存在时启动阶段自动创建
         */
        private String bucket;

        /**
         * 区域，MinIO 可随便填 us-east-1
         */
        private String region = "us-east-1";

        /**
         * MinIO 必须开启路径风格；阿里云 OSS、腾讯云 COS 也建议开启
         */
        private boolean pathStyleAccess = true;

        /**
         * 对外访问前缀（CDN 域名或桶的公网地址），为空时用 endpoint/bucket 拼接
         */
        private String publicBaseUrl;
    }
}
