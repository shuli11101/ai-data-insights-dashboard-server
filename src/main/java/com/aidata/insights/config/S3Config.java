package com.aidata.insights.config;

import com.aidata.insights.config.properties.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

/**
 * 对象存储客户端配置，仅在 app.upload.storage-type=s3 时装配
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.upload", name = "storage-type", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(UploadProperties uploadProperties) {
        UploadProperties.S3 s3 = uploadProperties.getS3();
        require(s3.getEndpoint(), "app.upload.s3.endpoint");
        require(s3.getAccessKey(), "app.upload.s3.access-key");
        require(s3.getSecretKey(), "app.upload.s3.secret-key");
        require(s3.getBucket(), "app.upload.s3.bucket");

        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())))
                // MinIO 必须使用路径风格访问
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.isPathStyleAccess())
                        .build())
                // 头像上传是低频同步操作，用 JDK HttpURLConnection 即可，避免引入 netty 线程池
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();

        ensureBucket(client, s3.getBucket());
        return client;
    }

    /**
     * 桶不存在时自动创建；对象存储暂时不可用只告警，不阻断应用启动
     */
    private void ensureBucket(S3Client client, String bucket) {
        try {
            client.headBucket(request -> request.bucket(bucket));
            log.info("对象存储桶已就绪: {}", bucket);
        } catch (NoSuchBucketException e) {
            try {
                client.createBucket(request -> request.bucket(bucket));
                log.info("已创建对象存储桶: {}", bucket);
            } catch (SdkException createError) {
                log.warn("创建对象存储桶 {} 失败: {}", bucket, createError.getMessage());
            }
        } catch (SdkException e) {
            log.warn("检查对象存储桶 {} 失败，请确认对象存储可用: {}", bucket, e.getMessage());
        }
    }

    private void require(String value, String key) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("app.upload.storage-type=s3 时必须配置 " + key);
        }
    }
}
