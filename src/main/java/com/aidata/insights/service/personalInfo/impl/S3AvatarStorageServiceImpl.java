package com.aidata.insights.service.personalInfo.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.config.properties.UploadProperties;
import com.aidata.insights.service.personalInfo.IAvatarStorageService;
import com.aidata.insights.service.personalInfo.support.AvatarFileValidator;
import com.aidata.insights.service.personalInfo.support.AvatarKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 头像存储实现：上传到 S3 协议对象存储（MinIO / 阿里云 OSS / 腾讯云 COS）
 *
 * <p>启用方式：app.upload.storage-type=s3</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.upload", name = "storage-type", havingValue = "s3")
public class S3AvatarStorageServiceImpl implements IAvatarStorageService {

    private static final String KEY_PREFIX = "avatar/";

    private final UploadProperties uploadProperties;

    private final AvatarFileValidator avatarFileValidator;

    private final S3Client s3Client;

    @Override
    public String store(Long userId, MultipartFile file) {
        AvatarFileValidator.ValidatedAvatar avatar = avatarFileValidator.validate(file);
        String key = KEY_PREFIX + AvatarKeys.next(userId, avatar.extension());

        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(uploadProperties.getS3().getBucket())
                    .key(key)
                    .contentType(contentType(avatar.extension()))
                    // 文件名带 UUID，内容不会变，可以长期缓存
                    .cacheControl("public, max-age=31536000, immutable")
                    .build(), RequestBody.fromBytes(avatar.content()));
        } catch (SdkException e) {
            log.error("头像上传对象存储失败: key={}", key, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "头像保存失败，请稍后重试");
        }

        return publicUrl(key);
    }

    /**
     * 对外访问地址：优先使用配置的 CDN / 公网前缀，否则用 endpoint/bucket 拼接
     */
    private String publicUrl(String key) {
        UploadProperties.S3 s3 = uploadProperties.getS3();
        if (StringUtils.hasText(s3.getPublicBaseUrl())) {
            return trimTrailingSlash(s3.getPublicBaseUrl()) + "/" + key;
        }
        return trimTrailingSlash(s3.getEndpoint()) + "/" + s3.getBucket() + "/" + key;
    }

    private String contentType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
